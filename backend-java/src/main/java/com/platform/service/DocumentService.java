package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.config.MinioConfig;
import com.platform.entity.document.Document;
import com.platform.exception.BusinessException;
import com.platform.exception.ErrorCode;
import com.platform.mapper.document.DocumentMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    public Page<Document> pageQuery(int page, int pageSize, String keyword, String fileType, String status) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Document::getFileName, keyword);
        }
        if (fileType != null && !fileType.isBlank()) {
            wrapper.eq(Document::getFileType, fileType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Document::getStatus, status);
        }
        wrapper.orderByDesc(Document::getCreatedAt);
        return documentMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Document getById(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return doc;
    }

    public Document upload(MultipartFile file, Long uploadUserId) {
        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String ext = getExtension(originalFilename).toLowerCase();
        if (!ext.matches("pdf|jpg|jpeg|png|doc|docx")) {
            throw new BusinessException(400, "不支持的文件类型: " + ext);
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过 20MB");
        }

        // 上传到 MinIO
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = "documents/" + datePath + "/" + UUID.randomUUID() + "." + ext;
        String bucket = minioConfig.getBucket();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        // 写入数据库
        String fileType = mapFileType(ext);
        Document doc = new Document();
        doc.setFileName(originalFilename);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setFileUrl("/" + bucket + "/" + objectName);
        doc.setUploadUserId(uploadUserId);
        doc.setStatus("pending");
        documentMapper.insert(doc);

        return doc;
    }

    public void delete(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 从 MinIO 删除文件
        try {
            String objectName = extractObjectName(doc.getFileUrl());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", doc.getFileUrl(), e);
        }

        documentMapper.deleteById(id);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }

    private String mapFileType(String ext) {
        return switch (ext) {
            case "pdf" -> "pdf";
            case "jpg", "jpeg", "png" -> "image";
            case "doc", "docx" -> "word";
            default -> ext;
        };
    }

    private String extractObjectName(String fileUrl) {
        // fileUrl format: /{bucket}/{objectName}
        int secondSlash = fileUrl.indexOf('/', 1);
        return secondSlash == -1 ? fileUrl.substring(1) : fileUrl.substring(secondSlash + 1);
    }
}

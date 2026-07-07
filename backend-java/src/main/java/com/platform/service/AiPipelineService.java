package com.platform.service;

import com.platform.config.MinioConfig;
import com.platform.entity.document.Document;
import com.platform.entity.document.DocumentLayout;
import com.platform.entity.document.DocumentOcr;
import com.platform.entity.document.DocumentTable;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.pipeline.ValidationResult;
import com.platform.mapper.document.DocumentLayoutMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.document.DocumentOcrMapper;
import com.platform.mapper.document.DocumentTableMapper;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import com.platform.mapper.pipeline.ValidationResultMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for calling Python AI service endpoints.
 * Handles preprocess, OCR, layout, table, and full pipeline execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPipelineService {

    private final RestTemplate aiRestTemplate;
    private final com.platform.config.AiClientConfig aiClientConfig;
    private final DocumentMapper documentMapper;
    private final DocumentLayoutMapper documentLayoutMapper;
    private final DocumentOcrMapper documentOcrMapper;
    private final DocumentTableMapper documentTableMapper;
    private final PipelineMapper pipelineMapper;
    private final ExtractionResultMapper extractionResultMapper;
    private final ValidationResultMapper validationResultMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    // ==================== Preprocess ====================

    /**
     * Send document to Python AI for preprocessing (deskew + stamp removal).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> preprocessDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        // Update status
        doc.setStatus("preprocessing");
        documentMapper.updateById(doc);

        try {
            byte[] fileBytes = downloadFileFromMinio(doc.getFileUrl());
            String filename = doc.getFileName();

            Map<String, Object> result = callAiPreprocess(fileBytes, filename);

            doc.setStatus("pending");
            documentMapper.updateById(doc);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", documentId);
            response.put("status", "pending");
            response.put("message", "预处理完成");
            response.put("aiResult", result);
            return response;

        } catch (Exception e) {
            log.error("Preprocess failed for document {}", documentId, e);
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", documentId);
            response.put("status", "failed");
            response.put("message", "预处理失败: " + e.getMessage());
            return response;
        }
    }

    // ==================== Full Pipeline ====================

    /**
     * Run the full AI pipeline on a document.
     * Creates pipeline record, calls Python AI, persists all results.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> runPipeline(Long documentId, List<Map<String, String>> metadataFields) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        // Create pipeline record
        Pipeline pipeline = new Pipeline();
        pipeline.setDocumentId(documentId);
        pipeline.setTaskId(java.util.UUID.randomUUID().toString());
        pipeline.setStatus("running");
        pipeline.setCurrentStep("preprocess");
        pipeline.setTotalSteps(5);
        pipeline.setCompletedSteps(0);
        pipeline.setStartedAt(LocalDateTime.now());
        pipelineMapper.insert(pipeline);

        try {
            byte[] fileBytes = downloadFileFromMinio(doc.getFileUrl());
            String filename = doc.getFileName();

            Map<String, Object> aiResult = callAiPipeline(fileBytes, filename, metadataFields, documentId);

            // Persist results
            persistLayoutResult(pipeline.getId(), (Map<String, Object>) aiResult.get("layout"));
            persistOcrResult(pipeline.getId(), (Map<String, Object>) aiResult.get("ocr"));
            persistTableResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("tables"));
            persistExtractionResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("extraction"));

            // Update pipeline status
            pipeline.setStatus("completed");
            pipeline.setCurrentStep("extract");
            pipeline.setCompletedSteps(5);
            pipeline.setFinishedAt(LocalDateTime.now());
            if (aiResult.get("duration") instanceof Number duration) {
                pipeline.setDuration(duration.doubleValue());
            }
            pipelineMapper.updateById(pipeline);

            // Update document status
            doc.setStatus("completed");
            documentMapper.updateById(doc);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("pipelineId", pipeline.getId());
            response.put("documentId", documentId);
            response.put("status", "completed");
            response.put("duration", pipeline.getDuration());
            response.put("message", "全流程处理完成");
            return response;

        } catch (Exception e) {
            log.error("Pipeline failed for document {}", documentId, e);
            pipeline.setStatus("failed");
            pipeline.setErrorMessage(e.getMessage());
            pipeline.setFinishedAt(LocalDateTime.now());
            pipelineMapper.updateById(pipeline);

            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("pipelineId", pipeline.getId());
            response.put("documentId", documentId);
            response.put("status", "failed");
            response.put("message", "处理失败: " + e.getMessage());
            return response;
        }
    }

    // ==================== Private: Call Python AI ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> callAiPreprocess(byte[] fileBytes, String filename) {
        String url = aiClientConfig.getBaseUrl() + "/api/ai/preprocess";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = aiRestTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            return (Map<String, Object>) responseBody.get("data");
        }
        return responseBody != null ? responseBody : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callAiPipeline(byte[] fileBytes, String filename,
                                                List<Map<String, String>> metadataFields,
                                                Long documentId) {
        String fieldsJson = "[]";
        if (metadataFields != null && !metadataFields.isEmpty()) {
            try {
                fieldsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadataFields);
            } catch (Exception ignored) {}
        }

        String url = aiClientConfig.getBaseUrl() + "/api/ai/pipeline/run" +
                "?metadata_fields=" + java.net.URLEncoder.encode(fieldsJson, java.nio.charset.StandardCharsets.UTF_8) +
                "&document_id=" + documentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = aiRestTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            return (Map<String, Object>) responseBody.get("data");
        }
        return responseBody != null ? responseBody : Collections.emptyMap();
    }

    // ==================== Private: MinIO Download ====================

    private byte[] downloadFileFromMinio(String fileUrl) throws Exception {
        // fileUrl format: /{bucket}/documents/{date}/{uuid}.ext
        String objectName = fileUrl;
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1); // remove leading /
            int slashIdx = objectName.indexOf('/');
            if (slashIdx > 0) {
                objectName = objectName.substring(slashIdx + 1); // remove bucket name
            }
        }

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    // ==================== Private: Persist Results ====================

    private void persistLayoutResult(Long pipelineId, Map<String, Object> layoutData) {
        if (layoutData == null || !layoutData.containsKey("regions")) return;

        DocumentLayout layout = new DocumentLayout();
        // Find document via pipeline
        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline != null) {
            layout.setDocumentId(pipeline.getDocumentId());
        }
        try {
            Object regions = layoutData.get("regions");
            layout.setRegions(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(regions));
        } catch (Exception e) {
            layout.setRegions("[]");
        }
        layout.setCreatedAt(LocalDateTime.now());
        documentLayoutMapper.insert(layout);
    }

    @SuppressWarnings("unchecked")
    private void persistOcrResult(Long pipelineId, Map<String, Object> ocrData) {
        if (ocrData == null) return;

        DocumentOcr ocr = new DocumentOcr();
        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline != null) {
            ocr.setDocumentId(pipeline.getDocumentId());
        }

        if (ocrData.get("total_chars") instanceof Number totalChars) {
            ocr.setTotalChars(totalChars.intValue());
        }
        if (ocrData.get("avg_confidence") instanceof Number avgConf) {
            ocr.setAvgConfidence(avgConf.doubleValue());
        }
        ocr.setLanguage("ch");

        try {
            Object entries = ocrData.get("entries");
            ocr.setEntries(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(entries));
        } catch (Exception e) {
            ocr.setEntries("[]");
        }
        ocr.setCreatedAt(LocalDateTime.now());
        documentOcrMapper.insert(ocr);
    }

    @SuppressWarnings("unchecked")
    private void persistTableResults(Long pipelineId, List<Map<String, Object>> tables) {
        if (tables == null || tables.isEmpty()) return;

        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline == null) return;

        for (Map<String, Object> tableData : tables) {
            DocumentTable table = new DocumentTable();
            table.setDocumentId(pipeline.getDocumentId());

            if (tableData.get("confidence") instanceof Number conf) {
                table.setConfidence(conf.doubleValue());
            }
            try {
                Object original = tableData.get("original_table");
                table.setOriginalTable(original != null ?
                        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(original) : "{}");
                Object extracted = tableData.get("extracted_table");
                table.setExtractedTable(extracted != null ?
                        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extracted) : "{}");
            } catch (Exception e) {
                table.setOriginalTable("{}");
                table.setExtractedTable("{}");
            }
            table.setCreatedAt(LocalDateTime.now());
            documentTableMapper.insert(table);
        }
    }

    @SuppressWarnings("unchecked")
    private void persistExtractionResults(Long pipelineId, List<Map<String, Object>> extractions) {
        if (extractions == null || extractions.isEmpty()) return;

        for (Map<String, Object> ext : extractions) {
            ExtractionResult result = new ExtractionResult();
            result.setPipelineId(pipelineId);
            result.setFieldCode(String.valueOf(ext.getOrDefault("field_code", ext.getOrDefault("code", ""))));
            result.setFieldName(String.valueOf(ext.getOrDefault("field_name", ext.getOrDefault("field_code", ""))));
            result.setExtractedValue(String.valueOf(ext.getOrDefault("value", "")));
            if (ext.get("confidence") instanceof Number conf) {
                result.setConfidence(conf.doubleValue());
            }
            result.setStatus("extracted");

            // pageRef is Integer type
            Object pageRefObj = ext.get("page_ref");
            if (pageRefObj != null) {
                try {
                    result.setPageRef(Integer.parseInt(String.valueOf(pageRefObj)));
                } catch (NumberFormatException ignored) {}
            }

            extractionResultMapper.insert(result);
        }
    }
}

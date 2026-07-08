package com.platform.service;

import com.platform.config.MinioConfig;
import com.platform.entity.document.Document;
import com.platform.entity.document.DocumentLayout;
import com.platform.entity.document.DocumentOcr;
import com.platform.entity.document.DocumentTable;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.pipeline.ReflectionResult;
import com.platform.entity.pipeline.ValidationResult;
import com.platform.mapper.document.DocumentLayoutMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.document.DocumentOcrMapper;
import com.platform.mapper.document.DocumentTableMapper;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import com.platform.mapper.pipeline.ReflectionResultMapper;
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
    private final ReflectionResultMapper reflectionResultMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    // ==================== Preprocess ====================

    /**
     * Send document to Python AI for preprocessing.
     * Python's DocumentConverter handles all file types (image, pdf, word).
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
            String fileType = doc.getFileType();

            Map<String, Object> result = callAiPreprocess(fileBytes, filename, fileType);

            doc.setStatus("preprocessed");
            // Update pageCount if returned
            if (result.get("page_count") instanceof Number pc) {
                doc.setPageCount(pc.intValue());
            }
            documentMapper.updateById(doc);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", documentId);
            response.put("status", "preprocessed");
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
        pipeline.setTotalSteps(7);
        pipeline.setCompletedSteps(0);
        pipeline.setStartedAt(LocalDateTime.now());
        pipelineMapper.insert(pipeline);

        log.info("Starting pipeline for document {} — metadata fields count: {}",
                documentId, metadataFields != null ? metadataFields.size() : 0);

        try {
            byte[] fileBytes = downloadFileFromMinio(doc.getFileUrl());
            String filename = doc.getFileName();

            Map<String, Object> aiResult = callAiPipeline(fileBytes, filename, metadataFields, documentId, doc.getFileType());

            // Build state graph from the pipeline timeline
            String stateGraph = buildStateGraph(aiResult);
            pipeline.setStateGraph(stateGraph);

            // Persist results
            persistLayoutResult(pipeline.getId(), (Map<String, Object>) aiResult.get("layout"));
            persistOcrResult(pipeline.getId(), (Map<String, Object>) aiResult.get("ocr"));
            persistTableResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("tables"));
            persistExtractionResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("extraction"));
            persistValidationResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("validations"));
            persistReflectionResults(pipeline.getId(), (List<Map<String, Object>>) aiResult.get("reflections"));

            // Update page count from pipeline result
            if (aiResult.get("page_count") instanceof Number pc) {
                doc.setPageCount(pc.intValue());
            }

            // Update pipeline status
            pipeline.setStatus("completed");
            pipeline.setCurrentStep("reflect");
            pipeline.setCompletedSteps(7);
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
    private Map<String, Object> callAiPreprocess(byte[] fileBytes, String filename, String fileType) {
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
        body.add("file_type", fileType != null ? fileType : "image");

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
                                                Long documentId, String fileType) {
        String fieldsJson = "[]";
        if (metadataFields != null && !metadataFields.isEmpty()) {
            try {
                fieldsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadataFields);
            } catch (Exception ignored) {}
        }

        // Ensure fileType is never null/empty — fallback to extension detection
        String resolvedFileType = resolveFileType(fileType, filename);

        String url = aiClientConfig.getBaseUrl() + "/api/ai/pipeline/run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("metadata_fields", fieldsJson);
        body.add("document_id", String.valueOf(documentId));
        body.add("file_type", resolvedFileType);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Calling Python AI: POST {} (metadataFields={}, fileType={}, resolvedFileType={}, filename={})",
                url, metadataFields != null ? metadataFields.size() : 0, fileType, resolvedFileType, filename);
        try {
            ResponseEntity<Map> response = aiRestTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            log.info("Python AI responded: status={}", response.getStatusCode());

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Python AI returned empty response");
            }

            // Check API-level error (code != 0/200)
            Object code = responseBody.get("code");
            if (code instanceof Number && ((Number) code).intValue() != 0
                    && ((Number) code).intValue() != 200) {
                String message = String.valueOf(responseBody.getOrDefault("message", "Unknown error"));
                log.error("Python AI returned error code={}, message={}", code, message);
                throw new RuntimeException("AI处理失败: " + message);
            }

            if (responseBody.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                log.info("Python AI result keys: {}", data.keySet());

                // Check if the AI returned a business error
                if (data.containsKey("error") && !data.containsKey("extraction")) {
                    String error = String.valueOf(data.get("error"));
                    log.error("Python AI pipeline returned error: {}", error);
                    throw new RuntimeException("AI处理失败: " + error);
                }

                return data;
            }
            return responseBody;
        } catch (Exception e) {
            log.error("Python AI call failed — url={}, error={}", url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Resolve file type with fallback to filename extension.
     * Ensures file_type is always a valid value (image/pdf/word/excel/html/text).
     */
    private String resolveFileType(String fileType, String filename) {
        // Use explicit type if valid
        if (fileType != null && !fileType.isBlank()) {
            String ft = fileType.toLowerCase().trim();
            if (ft.matches("image|pdf|word|excel|html|text|rtf")) {
                return ft;
            }
        }

        // Fallback: detect from filename extension
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "pdf" -> "pdf";
                case "doc", "docx" -> "word";
                case "xls", "xlsx" -> "excel";
                case "html", "htm" -> "html";
                case "txt", "csv", "md", "log" -> "text";
                case "rtf" -> "rtf";
                case "jpg", "jpeg", "png", "bmp", "tiff", "tif", "webp", "gif" -> "image";
                default -> "image";
            };
        }

        log.warn("Unable to resolve file type for file='{}', fileType='{}'; defaulting to 'image'",
                filename, fileType);
        return "image";
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

    @SuppressWarnings("unchecked")
    private void persistValidationResults(Long pipelineId, List<Map<String, Object>> validations) {
        if (validations == null || validations.isEmpty()) return;

        for (Map<String, Object> v : validations) {
            ValidationResult result = new ValidationResult();
            result.setPipelineId(pipelineId);
            result.setFieldName(String.valueOf(v.getOrDefault("field_name", v.getOrDefault("field", ""))));
            result.setRule(String.valueOf(v.getOrDefault("rule", "")));
            result.setSeverity(String.valueOf(v.getOrDefault("severity", "info")));
            result.setMessage(String.valueOf(v.getOrDefault("message", "")));
            result.setSuggestion(String.valueOf(v.getOrDefault("suggestion", "")));
            validationResultMapper.insert(result);
        }
    }

    @SuppressWarnings("unchecked")
    private void persistReflectionResults(Long pipelineId, List<Map<String, Object>> reflections) {
        if (reflections == null || reflections.isEmpty()) return;

        for (Map<String, Object> r : reflections) {
            ReflectionResult result = new ReflectionResult();
            result.setPipelineId(pipelineId);
            result.setFieldName(String.valueOf(r.getOrDefault("field_name", r.getOrDefault("field", ""))));
            result.setBeforeValue(String.valueOf(r.getOrDefault("before_value", r.getOrDefault("beforeValue", ""))));
            result.setAfterValue(String.valueOf(r.getOrDefault("after_value", r.getOrDefault("afterValue", ""))));
            result.setReason(String.valueOf(r.getOrDefault("reason", "")));
            reflectionResultMapper.insert(result);
        }
    }

    /**
     * Build a state graph JSON from the pipeline timeline.
     * The graph shows nodes (stages) and edges (transitions) for frontend visualization.
     */
    private String buildStateGraph(Map<String, Object> aiResult) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Define the 7 pipeline stages
        String[] stages = {"preprocess", "layout", "ocr", "table", "extract", "validate", "reflect"};
        String[] labels = {"预处理", "版面分析", "OCR识别", "表格提取", "字段提取", "数据校验", "反思修正"};

        // Check timeline for stage statuses
        Object timelineObj = aiResult.get("timeline");
        Map<String, String> stageStatus = new LinkedHashMap<>();
        if (timelineObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> timeline = (List<Map<String, Object>>) timelineObj;
            for (Map<String, Object> entry : timeline) {
                String stage = String.valueOf(entry.getOrDefault("stage", ""));
                String status = String.valueOf(entry.getOrDefault("status", "done"));
                stageStatus.put(stage, status);
            }
        }

        for (int i = 0; i < stages.length; i++) {
            String stage = stages[i];
            String status = stageStatus.getOrDefault(stage, "pending");
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", stage);
            node.put("label", labels[i]);
            node.put("status", status);
            nodes.add(node);

            if (i > 0) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", stages[i - 1]);
                edge.put("to", stage);
                edges.add(edge);
            }
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(graph);
        } catch (Exception e) {
            log.warn("Failed to serialize stateGraph", e);
            return "{}";
        }
    }
}

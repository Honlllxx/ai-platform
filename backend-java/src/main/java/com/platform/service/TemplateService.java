package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.entity.business.Template;
import com.platform.exception.BusinessException;
import com.platform.exception.ErrorCode;
import com.platform.mapper.business.TemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    public Page<Template> pageQuery(int page, int pageSize, String documentType, String keyword, Integer status) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(Template::getDocumentType, documentType);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Template::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(Template::getStatus, status);
        }
        wrapper.orderByDesc(Template::getCreatedAt);
        return templateMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Template getById(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return template;
    }

    public Template create(Template template) {
        if (template.getStatus() == null) template.setStatus(1);
        if (template.getVersion() == null) template.setVersion(1);
        templateMapper.insert(template);
        return template;
    }

    public Template update(Long id, Template request) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (request.getName() != null) template.setName(request.getName());
        if (request.getDocumentType() != null) template.setDocumentType(request.getDocumentType());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getRegions() != null) template.setRegions(request.getRegions());
        if (request.getStatus() != null) template.setStatus(request.getStatus());
        templateMapper.updateById(template);
        return template;
    }

    public void delete(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        templateMapper.deleteById(id);
    }

    public List<Map<String, Object>> parseRegions(String regionsJson) {
        if (regionsJson == null || regionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(regionsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}

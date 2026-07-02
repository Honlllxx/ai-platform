package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.business.MetadataField;
import com.platform.exception.BusinessException;
import com.platform.exception.ErrorCode;
import com.platform.mapper.business.MetadataFieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataFieldService {

    private final MetadataFieldMapper metadataFieldMapper;

    public Page<MetadataField> pageQuery(int page, int pageSize, String groupCode, String keyword, Integer status) {
        LambdaQueryWrapper<MetadataField> wrapper = new LambdaQueryWrapper<>();
        if (groupCode != null && !groupCode.isBlank()) {
            wrapper.eq(MetadataField::getGroupCode, groupCode);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(MetadataField::getName, keyword)
                    .or().like(MetadataField::getCode, keyword));
        }
        if (status != null) {
            wrapper.eq(MetadataField::getStatus, status);
        }
        wrapper.orderByAsc(MetadataField::getSort);
        return metadataFieldMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public MetadataField getById(Long id) {
        MetadataField field = metadataFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return field;
    }

    public MetadataField create(MetadataField field) {
        Long count = metadataFieldMapper.selectCount(
                new LambdaQueryWrapper<MetadataField>().eq(MetadataField::getCode, field.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (field.getStatus() == null) field.setStatus(1);
        if (field.getRequired() == null) field.setRequired(false);
        metadataFieldMapper.insert(field);
        return field;
    }

    public MetadataField update(Long id, MetadataField request) {
        MetadataField field = metadataFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (request.getName() != null) field.setName(request.getName());
        if (request.getType() != null) field.setType(request.getType());
        if (request.getLength() != null) field.setLength(request.getLength());
        if (request.getEnumValues() != null) field.setEnumValues(request.getEnumValues());
        if (request.getValidationRule() != null) field.setValidationRule(request.getValidationRule());
        if (request.getGroupCode() != null) field.setGroupCode(request.getGroupCode());
        if (request.getGroupName() != null) field.setGroupName(request.getGroupName());
        if (request.getRequired() != null) field.setRequired(request.getRequired());
        if (request.getDescription() != null) field.setDescription(request.getDescription());
        if (request.getSort() != null) field.setSort(request.getSort());
        if (request.getStatus() != null) field.setStatus(request.getStatus());
        metadataFieldMapper.updateById(field);
        return field;
    }

    public void delete(Long id) {
        MetadataField field = metadataFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        metadataFieldMapper.deleteById(id);
    }

    public List<MetadataField> getByGroupCode(String groupCode) {
        return metadataFieldMapper.selectList(
                new LambdaQueryWrapper<MetadataField>()
                        .eq(MetadataField::getGroupCode, groupCode)
                        .eq(MetadataField::getStatus, 1)
                        .orderByAsc(MetadataField::getSort));
    }

    public List<String> getGroups() {
        return metadataFieldMapper.selectList(null).stream()
                .map(MetadataField::getGroupCode)
                .distinct()
                .toList();
    }
}

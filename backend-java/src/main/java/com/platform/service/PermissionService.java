package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.entity.system.Permission;
import com.platform.exception.BusinessException;
import com.platform.exception.ErrorCode;
import com.platform.mapper.system.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public List<Permission> listAll() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
    }

    public Permission getById(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permission;
    }

    public List<Map<String, Object>> getTree() {
        List<Permission> all = listAll();
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();

        List<Map<String, Object>> roots = new ArrayList<>();
        List<Map<String, Object>> allMaps = new ArrayList<>();

        for (Permission p : all) {
            Map<String, Object> map = permissionToMap(p);
            map.put("children", new ArrayList<>());
            allMaps.add(map);

            Long parentId = p.getParentId();
            if (parentId == null) {
                roots.add(map);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(map);
            }
        }

        for (Map<String, Object> map : allMaps) {
            Long id = (Long) map.get("id");
            List<Map<String, Object>> children = childrenMap.get(id);
            if (children != null) {
                map.put("children", children);
            }
        }

        return roots;
    }

    public Permission create(Permission permission) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>().eq(Permission::getCode, permission.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PERMISSION_CODE_EXISTS);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        permissionMapper.insert(permission);
        return permission;
    }

    public Permission update(Long id, Permission request) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        if (request.getName() != null) permission.setName(request.getName());
        if (request.getType() != null) permission.setType(request.getType());
        if (request.getParentId() != null) permission.setParentId(request.getParentId());
        if (request.getPath() != null) permission.setPath(request.getPath());
        if (request.getIcon() != null) permission.setIcon(request.getIcon());
        if (request.getSort() != null) permission.setSort(request.getSort());
        if (request.getStatus() != null) permission.setStatus(request.getStatus());
        permissionMapper.updateById(permission);
        return permission;
    }

    public void delete(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        Long childCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PERMISSION_HAS_CHILDREN);
        }
        permissionMapper.deleteById(id);
    }

    private Map<String, Object> permissionToMap(Permission p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getName());
        map.put("code", p.getCode());
        map.put("type", p.getType());
        map.put("parentId", p.getParentId());
        map.put("path", p.getPath());
        map.put("icon", p.getIcon());
        map.put("sort", p.getSort());
        map.put("status", p.getStatus());
        map.put("createTime", p.getCreatedAt());
        return map;
    }
}

package com.platform.controller;

import com.platform.dto.ApiResponse;
import com.platform.entity.system.Permission;
import com.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_permission:list')")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.success(permissionService.getTree());
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('ROLE_permission:list')")
    public ApiResponse<List<Map<String, Object>>> treeEndpoint() {
        return ApiResponse.success(permissionService.getTree());
    }

    @GetMapping("/flat")
    @PreAuthorize("hasAuthority('ROLE_permission:list')")
    public ApiResponse<List<Map<String, Object>>> flat() {
        List<Permission> list = permissionService.listAll();
        List<Map<String, Object>> records = list.stream().map(p -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
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
        }).collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_permission:list')")
    public ApiResponse<Permission> detail(@PathVariable Long id) {
        return ApiResponse.success(permissionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_permission:create')")
    public ApiResponse<Permission> create(@RequestBody Permission permission) {
        return ApiResponse.success(permissionService.create(permission));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_permission:update')")
    public ApiResponse<Permission> update(@PathVariable Long id, @RequestBody Permission permission) {
        return ApiResponse.success(permissionService.update(id, permission));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_permission:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }
}

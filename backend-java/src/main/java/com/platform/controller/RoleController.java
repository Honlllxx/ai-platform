package com.platform.controller;

import com.platform.dto.ApiResponse;
import com.platform.dto.role.CreateRoleRequest;
import com.platform.dto.role.UpdateRoleRequest;
import com.platform.entity.system.Role;
import com.platform.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_role:list')")
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Role> roles = roleService.listAll();
        List<Map<String, Object>> records = roles.stream().map(role -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getId());
            map.put("name", role.getName());
            map.put("code", role.getCode());
            map.put("description", role.getDescription());
            map.put("status", role.getStatus());
            map.put("permissionIds", roleService.getPermissionIds(role.getId()));
            map.put("userCount", roleService.countUsers(role.getId()));
            map.put("createTime", role.getCreatedAt());
            return map;
        }).toList();
        return ApiResponse.success(records);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_role:list')")
    public ApiResponse<List<Role>> all() {
        return ApiResponse.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_role:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Role role = roleService.getById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("code", role.getCode());
        map.put("description", role.getDescription());
        map.put("status", role.getStatus());
        map.put("permissionIds", roleService.getPermissionIds(role.getId()));
        map.put("createTime", role.getCreatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_role:create')")
    public ApiResponse<Role> create(@Valid @RequestBody CreateRoleRequest request) {
        Role role = roleService.createRole(request);
        return ApiResponse.success(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_role:update')")
    public ApiResponse<Role> update(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        Role role = roleService.updateRole(id, request);
        return ApiResponse.success(role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_role:update')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        roleService.updateStatus(id, body.get("status"));
        return ApiResponse.success();
    }

    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('ROLE_role:update')")
    public ApiResponse<Void> assignPermissions(@PathVariable Long roleId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> permissionIds = (List<Integer>) body.getOrDefault("permissionIds", java.util.Collections.emptyList());
        roleService.updateRolePermissions(roleId, permissionIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
        return ApiResponse.success();
    }
}

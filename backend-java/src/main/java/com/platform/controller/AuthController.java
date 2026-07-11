package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.dto.ApiResponse;
import com.platform.dto.LoginRequest;
import com.platform.dto.LoginResponse;
import com.platform.dto.RefreshTokenRequest;
import com.platform.entity.system.*;
import com.platform.mapper.system.*;
import com.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public Map<String, Object> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("token", response.getAccessToken());
        result.put("refreshToken", response.getRefreshToken());
        result.put("message", "success");
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        authService.logout(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", null);
        result.put("message", "success");
        return result;
    }

    @GetMapping("/current-user")
    public Map<String, Object> currentUser(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        User user = userMapper.selectById(userId);
        if (user == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 401);
            error.put("data", null);
            error.put("message", "用户不存在");
            return error;
        }

        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        List<Role> roles = roleIds.isEmpty() ? Collections.emptyList()
                : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Set<String> permissions = new HashSet<>();
        if (roleCodes.contains("admin")) {
            List<Permission> allPermissions = permissionMapper.selectList(null);
            permissions = allPermissions.stream().map(Permission::getCode).collect(Collectors.toSet());
        } else if (!roleIds.isEmpty()) {
            List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
            List<Long> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
            if (!permissionIds.isEmpty()) {
                List<Permission> permList = permissionMapper.selectBatchIds(permissionIds);
                permissions = permList.stream().map(Permission::getCode).collect(Collectors.toSet());
            }
        }

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("userId", user.getId());
        userData.put("username", user.getUsername());
        userData.put("nickname", user.getNickname());
        userData.put("avatar", user.getAvatar());
        userData.put("roles", roleCodes);
        userData.put("permissions", new ArrayList<>(permissions));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", userData);
        result.put("message", "success");
        return result;
    }
}

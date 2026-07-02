package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.dto.user.CreateUserRequest;
import com.platform.dto.user.ResetPasswordRequest;
import com.platform.dto.user.UpdateUserRequest;
import com.platform.entity.system.Role;
import com.platform.entity.system.User;
import com.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_user:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {

        Page<User> userPage = userService.pageQuery(page, pageSize, username, status);

        List<Map<String, Object>> records = userPage.getRecords().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("avatar", user.getAvatar());
            map.put("status", user.getStatus());
            map.put("createTime", user.getCreatedAt());

            List<Role> roles = userService.getUserRoles(user.getId());
            map.put("roleIds", roles.stream().map(Role::getId).collect(Collectors.toList()));
            map.put("roleNames", roles.stream().map(Role::getName).collect(Collectors.toList()));

            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, userPage.getTotal(), userPage.getCurrent(), userPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_user:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        List<Role> roles = userService.getUserRoles(id);

        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("avatar", user.getAvatar());
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreatedAt());
        map.put("roleIds", roles.stream().map(Role::getId).collect(Collectors.toList()));
        map.put("roleNames", roles.stream().map(Role::getName).collect(Collectors.toList()));

        return ApiResponse.success(map);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_user:create')")
    public ApiResponse<User> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ApiResponse.success(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_user:update')")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(id, request);
        return ApiResponse.success(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_user:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_user:update')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return ApiResponse.success();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('ROLE_user:update')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }
}

package com.platform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.entity.system.*;
import com.platform.mapper.system.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 2. 查角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        List<Role> roles = Collections.emptyList();
        Set<String> authorities = new HashSet<>();

        if (!roleIds.isEmpty()) {
            roles = roleMapper.selectBatchIds(roleIds);
            Set<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());

            // admin 角色拥有所有权限
            if (roleCodes.contains("admin")) {
                List<Permission> allPermissions = permissionMapper.selectList(null);
                authorities.addAll(allPermissions.stream()
                        .map(p -> "ROLE_" + p.getCode())
                        .collect(Collectors.toSet()));
            } else {
                // 3. 查权限
                List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
                List<Long> permissionIds = rolePermissions.stream()
                        .map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());

                if (!permissionIds.isEmpty()) {
                    List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
                    authorities.addAll(permissions.stream()
                            .map(p -> "ROLE_" + p.getCode())
                            .collect(Collectors.toSet()));
                }
            }
        }

        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() != null && user.getStatus() == 1,
                true, true, true,
                grantedAuthorities
        );
    }
}

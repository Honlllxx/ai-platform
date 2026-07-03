package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.dto.LoginRequest;
import com.platform.dto.LoginResponse;
import com.platform.entity.system.*;
import com.platform.mapper.system.*;
import com.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BadCredentialsException("用户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 获取角色和权限
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        List<Role> roles = roleIds.isEmpty() ? Collections.emptyList()
                : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Set<String> authoritySet = getAuthorities(roleCodes, roleIds);

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), authoritySet);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 存 Redis
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + user.getId(), refreshToken,
                7, TimeUnit.DAYS);

        // 构建用户信息
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roles(roleCodes)
                .permissions(new ArrayList<>(authoritySet))
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7200000)
                .userInfo(userInfo)
                .build();
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("refreshToken 无效或已过期");
        }
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 校验 Redis 中是否存在
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new BadCredentialsException("refreshToken 已失效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BadCredentialsException("用户不存在或已禁用");
        }

        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleIds.isEmpty() ? Collections.emptyList()
                : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());

        Set<String> authoritySet = getAuthorities(roleCodes, roleIds);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), authoritySet);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, newRefreshToken,
                7, TimeUnit.DAYS);

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roles(roleCodes)
                .permissions(new ArrayList<>(authoritySet))
                .build();

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(7200000)
                .userInfo(userInfo)
                .build();
    }

    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    private Set<String> getAuthorities(List<String> roleCodes, List<Long> roleIds) {
        Set<String> authorities = new HashSet<>();
        if (roleCodes.contains("admin")) {
            List<Permission> allPermissions = permissionMapper.selectList(null);
            authorities.addAll(allPermissions.stream()
                    .map(p -> p.getCode())
                    .collect(Collectors.toSet()));
        } else if (!roleIds.isEmpty()) {
            List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
            List<Long> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
            if (!permissionIds.isEmpty()) {
                List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
                authorities.addAll(permissions.stream()
                        .map(p -> p.getCode())
                        .collect(Collectors.toSet()));
            }
        }
        return authorities;
    }
}

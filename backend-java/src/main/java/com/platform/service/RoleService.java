package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.dto.role.CreateRoleRequest;
import com.platform.dto.role.UpdateRoleRequest;
import com.platform.entity.system.*;
import com.platform.exception.BusinessException;
import com.platform.exception.ErrorCode;
import com.platform.mapper.system.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    public List<Role> listAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByDesc(Role::getCreatedAt));
    }

    public Role getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    public List<Long> getPermissionIds(Long roleId) {
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        return rps.stream().map(RolePermission::getPermissionId).toList();
    }

    public long countUsers(Long roleId) {
        return userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
    }

    @Transactional
    public Role createRole(CreateRoleRequest request) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        roleMapper.insert(role);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        return role;
    }

    @Transactional
    public Role updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        roleMapper.updateById(role);

        if (request.getPermissionIds() != null) {
            rolePermissionMapper.delete(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
            if (!request.getPermissionIds().isEmpty()) {
                assignPermissions(id, request.getPermissionIds());
            }
        }

        return role;
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        long userCount = countUsers(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS);
        }

        roleMapper.deleteById(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
    }

    public void updateStatus(Long id, Integer status) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        role.setStatus(status);
        roleMapper.updateById(role);
    }

    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        for (Long permId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }
    }
}

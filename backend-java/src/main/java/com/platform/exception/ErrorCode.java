package com.platform.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证相关
    LOGIN_FAILED(1001, "用户名或密码错误"),
    TOKEN_EXPIRED(1002, "Token 已过期"),
    TOKEN_INVALID(1003, "Token 无效"),

    // 用户相关
    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_EXISTS(2002, "用户名已存在"),
    USER_DISABLED(2003, "用户已被禁用"),

    // 角色相关
    ROLE_NOT_FOUND(3001, "角色不存在"),
    ROLE_CODE_EXISTS(3002, "角色编码已存在"),
    ROLE_HAS_USERS(3003, "角色下存在用户，无法删除"),

    // 权限相关
    PERMISSION_NOT_FOUND(4001, "权限不存在"),
    PERMISSION_CODE_EXISTS(4002, "权限编码已存在"),
    PERMISSION_HAS_CHILDREN(4003, "权限存在子节点，无法删除");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

package com.platform.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.entity.log.OperationLog;
import com.platform.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.platform.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        com.platform.annotation.OperationLog opLogAnnotation = getAnnotation(joinPoint);
        long start = System.currentTimeMillis();

        OperationLog entity = new OperationLog();
        entity.setModule(opLogAnnotation.value());
        entity.setOperationType(opLogAnnotation.type());

        try {
            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                entity.setUsername(auth.getName());
            }

            // 获取请求 IP
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entity.setIp(getClientIp(request));

                // 记录请求参数
                Object[] args = joinPoint.getArgs();
                try {
                    entity.setRequestParams(truncate(objectMapper.writeValueAsString(args), 2000));
                } catch (Exception ignored) {}
            }

            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            entity.setDuration((int) duration);
            entity.setStatus("success");

            // 记录响应
            try {
                entity.setResponseResult(truncate(objectMapper.writeValueAsString(result), 1000));
            } catch (Exception ignored) {}

            operationLogService.save(entity);

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            entity.setDuration((int) duration);
            entity.setStatus("fail");
            operationLogService.save(entity);
            throw e;
        }
    }

    private com.platform.annotation.OperationLog getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(com.platform.annotation.OperationLog.class);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}

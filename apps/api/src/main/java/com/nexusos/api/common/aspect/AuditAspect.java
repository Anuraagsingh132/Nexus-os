package com.nexusos.api.common.aspect;

import com.nexusos.api.common.domain.AuditLog;
import com.nexusos.api.common.repository.AuditLogRepository;
import com.nexusos.api.identity.security.CustomUserDetails;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void deleteMapping() {}

    @AfterReturning("deleteMapping()")
    public void logDeleteAction(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
        }

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String action = "DELETE via " + className + "." + methodName;

        AuditLog log = new AuditLog(action, userId, Instant.now());
        auditLogRepository.save(log);
    }
}

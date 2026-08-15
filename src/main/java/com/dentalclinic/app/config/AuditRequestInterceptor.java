package com.dentalclinic.app.config;

import com.dentalclinic.app.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AuditRequestInterceptor implements HandlerInterceptor {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final AuditEventService auditEventService;

    public AuditRequestInterceptor(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase()) || isSecurityEndpoint(request)) {
            return;
        }
        String eventType = handler instanceof HandlerMethod method
                ? method.getBeanType().getSimpleName() + "." + method.getMethod().getName()
                : "HTTP_MUTATION";
        String outcome = exception == null && response.getStatus() < 400 ? "SUCCESS" : "FAILURE";
        String details = exception == null ? null : exception.getClass().getSimpleName();
        auditEventService.record(eventType, currentActor(), outcome, request, response.getStatus(), details);
    }

    private boolean isSecurityEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/login".equals(path) || "/logout".equals(path) || path.startsWith("/actuator/");
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "PUBLIC";
        }
        return authentication.getName();
    }
}

package com.dentalclinic.app.config;

import com.dentalclinic.app.service.AuditEventService;
import com.dentalclinic.app.service.LoginThrottleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoginThrottleFilter extends OncePerRequestFilter {
    private final LoginThrottleService throttleService;
    private final AuditEventService auditEventService;

    public LoginThrottleFilter(LoginThrottleService throttleService, AuditEventService auditEventService) {
        this.throttleService = throttleService;
        this.auditEventService = auditEventService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isLoginSubmission(request)) {
            String username = request.getParameter("username");
            if (throttleService.isBlocked(username, request)) {
                auditEventService.record("LOGIN_BLOCKED", username, "BLOCKED", request, 429,
                        "Too many failed authentication attempts");
                response.sendRedirect(request.getContextPath() + "/login?blocked");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginSubmission(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && (request.getContextPath() + "/login").equals(request.getRequestURI());
    }
}

package com.dentalclinic.app.config;

import com.dentalclinic.app.service.AuditEventService;
import com.dentalclinic.app.service.LoginThrottleService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ClinicAuthenticationFailureHandler implements org.springframework.security.web.authentication.AuthenticationFailureHandler {
    private final LoginThrottleService throttleService;
    private final AuditEventService auditEventService;

    public ClinicAuthenticationFailureHandler(LoginThrottleService throttleService,
                                              AuditEventService auditEventService) {
        this.throttleService = throttleService;
        this.auditEventService = auditEventService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        boolean blocked = throttleService.recordFailure(username, request);
        auditEventService.record("LOGIN_FAILURE", username, blocked ? "BLOCKED" : "FAILURE",
                request, blocked ? 429 : 401, exception.getClass().getSimpleName());
        response.sendRedirect(request.getContextPath() + (blocked ? "/login?blocked" : "/login?error"));
    }
}

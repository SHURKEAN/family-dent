package com.dentalclinic.app.config;

import com.dentalclinic.app.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class ClinicLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
    private final AuditEventService auditEventService;

    public ClinicLogoutSuccessHandler(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
        setDefaultTargetUrl("/login?logout");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws java.io.IOException, jakarta.servlet.ServletException {
        String actor = authentication == null ? "UNKNOWN" : authentication.getName();
        auditEventService.record("LOGOUT", actor, "SUCCESS", request, 302, null);
        super.onLogoutSuccess(request, response, authentication);
    }
}

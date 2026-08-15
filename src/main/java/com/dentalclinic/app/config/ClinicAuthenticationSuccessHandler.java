package com.dentalclinic.app.config;

import com.dentalclinic.app.service.AuditEventService;
import com.dentalclinic.app.service.LoginThrottleService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ClinicAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final LoginThrottleService throttleService;
    private final AuditEventService auditEventService;

    public ClinicAuthenticationSuccessHandler(LoginThrottleService throttleService,
                                              AuditEventService auditEventService) {
        this.throttleService = throttleService;
        this.auditEventService = auditEventService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        throttleService.recordSuccess(authentication.getName(), request);
        auditEventService.record("LOGIN_SUCCESS", authentication.getName(), "SUCCESS", request, 302, null);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

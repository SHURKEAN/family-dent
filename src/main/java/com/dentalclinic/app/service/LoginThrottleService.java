package com.dentalclinic.app.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class LoginThrottleService {
    private final PersistentRateLimitService rateLimitService;
    private final int maximumFailures;
    private final int maximumFailuresPerIp;
    private final Duration window;
    private final Duration blockDuration;

    public LoginThrottleService(PersistentRateLimitService rateLimitService,
                                @Value("${clinic.security.login.maximum-failures:5}") int maximumFailures,
                                @Value("${clinic.security.login.maximum-failures-per-ip:20}") int maximumFailuresPerIp,
                                @Value("${clinic.security.login.window-minutes:15}") int windowMinutes,
                                @Value("${clinic.security.login.block-minutes:15}") int blockMinutes) {
        if (maximumFailures < 1 || maximumFailuresPerIp < maximumFailures || windowMinutes < 1 || blockMinutes < 1) {
            throw new IllegalStateException("Login throttling settings are invalid");
        }
        this.rateLimitService = rateLimitService;
        this.maximumFailures = maximumFailures;
        this.maximumFailuresPerIp = maximumFailuresPerIp;
        this.window = Duration.ofMinutes(windowMinutes);
        this.blockDuration = Duration.ofMinutes(blockMinutes);
    }

    public boolean isBlocked(String username, HttpServletRequest request) {
        return rateLimitService.isBlocked("LOGIN_ACCOUNT_CLIENT", accountClient(username, request))
                || rateLimitService.isBlocked("LOGIN_CLIENT", client(request));
    }

    public boolean recordFailure(String username, HttpServletRequest request) {
        boolean accountBlocked = rateLimitService.recordFailure("LOGIN_ACCOUNT_CLIENT",
                accountClient(username, request), maximumFailures, window, blockDuration);
        boolean clientBlocked = rateLimitService.recordFailure("LOGIN_CLIENT",
                client(request), maximumFailuresPerIp, window, blockDuration);
        return accountBlocked || clientBlocked;
    }

    public void recordSuccess(String username, HttpServletRequest request) {
        rateLimitService.reset("LOGIN_ACCOUNT_CLIENT", accountClient(username, request));
    }

    private String accountClient(String username, HttpServletRequest request) {
        return normalizeUsername(username) + "|" + client(request);
    }

    private String client(HttpServletRequest request) {
        String address = request == null ? null : request.getRemoteAddr();
        return address == null || address.isBlank() ? "unknown" : address.trim();
    }

    private String normalizeUsername(String username) {
        return username == null || username.isBlank()
                ? "unknown"
                : username.trim().toLowerCase(Locale.ROOT);
    }
}

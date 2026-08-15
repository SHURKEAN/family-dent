package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.AuditEvent;
import com.dentalclinic.app.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditEventService {
    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private final AuditEventRepository repository;
    private final SecurityFingerprintService fingerprintService;
    private final int retentionDays;

    public AuditEventService(AuditEventRepository repository,
                             SecurityFingerprintService fingerprintService,
                             @Value("${clinic.security.audit-retention-days:365}") int retentionDays) {
        if (retentionDays < 30) {
            throw new IllegalStateException("Audit retention must be at least 30 days");
        }
        this.repository = repository;
        this.fingerprintService = fingerprintService;
        this.retentionDays = retentionDays;
    }

    public List<AuditEvent> recent() {
        return repository.findTop250ByOrderByOccurredAtDesc();
    }

    public void record(String eventType, String actor, String outcome, HttpServletRequest request,
                       Integer statusCode, String details) {
        try {
            AuditEvent event = new AuditEvent();
            event.setEventType(limit(eventType, 100, "UNKNOWN"));
            event.setActorUsername(limit(actor, 80, "PUBLIC"));
            event.setOutcome(limit(outcome, 20, "UNKNOWN"));
            if (request != null) {
                event.setRequestMethod(limit(request.getMethod(), 10, null));
                event.setRequestPath(limit(request.getRequestURI(), 300, null));
                event.setClientFingerprint(fingerprintService.fingerprint("AUDIT_CLIENT", request.getRemoteAddr()));
            }
            event.setStatusCode(statusCode);
            event.setDetails(limit(details, 500, null));
            repository.saveAndFlush(event);
        } catch (RuntimeException exception) {
            log.error("Audit event could not be persisted: type={}, outcome={}", eventType, outcome, exception);
        }
    }

    @Scheduled(cron = "0 40 3 * * *")
    @Transactional
    public void removeExpiredEvents() {
        repository.deleteByOccurredAtBefore(LocalDateTime.now().minusDays(retentionDays));
    }

    private String limit(String value, int maximum, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maximum ? trimmed : trimmed.substring(0, maximum);
    }
}

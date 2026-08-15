package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.SecurityRateLimit;
import com.dentalclinic.app.repository.SecurityRateLimitRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PersistentRateLimitService {
    private final SecurityRateLimitRepository repository;
    private final SecurityFingerprintService fingerprintService;

    public PersistentRateLimitService(SecurityRateLimitRepository repository,
                                      SecurityFingerprintService fingerprintService) {
        this.repository = repository;
        this.fingerprintService = fingerprintService;
    }

    @Transactional
    public synchronized boolean tryConsume(String category, String subject, int maximum, Duration window) {
        validate(maximum, window);
        LocalDateTime now = LocalDateTime.now();
        SecurityRateLimit bucket = findOrCreate(category, subject, now);
        resetExpiredWindow(bucket, now, window);
        if (isBlocked(bucket, now) || bucket.getRequestCount() >= maximum) {
            bucket.setBlockedUntil(bucket.getWindowStartedAt().plus(window));
            bucket.setUpdatedAt(now);
            return false;
        }
        bucket.setRequestCount(bucket.getRequestCount() + 1);
        bucket.setUpdatedAt(now);
        return true;
    }

    @Transactional
    public synchronized boolean recordFailure(String category, String subject, int maximum,
                                              Duration window, Duration blockDuration) {
        validate(maximum, window);
        LocalDateTime now = LocalDateTime.now();
        SecurityRateLimit bucket = findOrCreate(category, subject, now);
        resetExpiredWindow(bucket, now, window);
        bucket.setRequestCount(bucket.getRequestCount() + 1);
        if (bucket.getRequestCount() >= maximum) {
            bucket.setBlockedUntil(now.plus(blockDuration));
        }
        bucket.setUpdatedAt(now);
        return isBlocked(bucket, now);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String category, String subject) {
        String key = fingerprintService.fingerprint(category, subject);
        return repository.findById(key)
                .map(bucket -> isBlocked(bucket, LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void reset(String category, String subject) {
        repository.deleteById(fingerprintService.fingerprint(category, subject));
    }

    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void removeOldBuckets() {
        repository.deleteByUpdatedAtBefore(LocalDateTime.now().minusDays(7));
    }

    private SecurityRateLimit findOrCreate(String category, String subject, LocalDateTime now) {
        String key = fingerprintService.fingerprint(category, subject);
        return repository.findForUpdate(key).orElseGet(() -> {
            SecurityRateLimit created = new SecurityRateLimit();
            created.setBucketKey(key);
            created.setCategory(category);
            created.setRequestCount(0);
            created.setWindowStartedAt(now);
            created.setUpdatedAt(now);
            return repository.save(created);
        });
    }

    private void resetExpiredWindow(SecurityRateLimit bucket, LocalDateTime now, Duration window) {
        if (!bucket.getWindowStartedAt().plus(window).isAfter(now)) {
            bucket.setRequestCount(0);
            bucket.setWindowStartedAt(now);
            bucket.setBlockedUntil(null);
        }
    }

    private boolean isBlocked(SecurityRateLimit bucket, LocalDateTime now) {
        return bucket.getBlockedUntil() != null && bucket.getBlockedUntil().isAfter(now);
    }

    private void validate(int maximum, Duration window) {
        if (maximum < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit values must be positive");
        }
    }
}

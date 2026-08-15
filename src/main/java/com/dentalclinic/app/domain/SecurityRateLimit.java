package com.dentalclinic.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_rate_limits")
public class SecurityRateLimit {
    @Id
    @Column(name = "bucket_key", length = 64)
    private String bucketKey;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getBucketKey() { return bucketKey; }
    public void setBucketKey(String bucketKey) { this.bucketKey = bucketKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public LocalDateTime getWindowStartedAt() { return windowStartedAt; }
    public void setWindowStartedAt(LocalDateTime windowStartedAt) { this.windowStartedAt = windowStartedAt; }
    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

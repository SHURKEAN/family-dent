CREATE TABLE security_rate_limits (
    bucket_key VARCHAR(64) PRIMARY KEY,
    category VARCHAR(30) NOT NULL,
    request_count INTEGER NOT NULL,
    window_started_at TIMESTAMP NOT NULL,
    blocked_until TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_rate_limit_count CHECK (request_count >= 0)
);

CREATE INDEX idx_rate_limits_updated ON security_rate_limits (updated_at);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    actor_username VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    request_method VARCHAR(10),
    request_path VARCHAR(300),
    status_code INTEGER,
    client_fingerprint VARCHAR(64),
    details VARCHAR(500)
);

CREATE INDEX idx_audit_events_occurred ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_actor ON audit_events (actor_username, occurred_at DESC);
CREATE INDEX idx_audit_events_type ON audit_events (event_type, occurred_at DESC);

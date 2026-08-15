ALTER TABLE patients ALTER COLUMN date_of_birth DROP NOT NULL;

CREATE TABLE dentist_working_hours (
    id BIGSERIAL PRIMARY KEY,
    dentist_id BIGINT NOT NULL REFERENCES clinic_users(id),
    day_of_week VARCHAR(12) NOT NULL CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_dentist_working_day UNIQUE (dentist_id, day_of_week),
    CONSTRAINT chk_working_hours_times CHECK (end_time > start_time),
    CONSTRAINT chk_slot_duration CHECK (slot_duration_minutes BETWEEN 10 AND 180)
);

CREATE INDEX idx_working_hours_dentist ON dentist_working_hours (dentist_id, day_of_week);

CREATE TABLE appointment_requests (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    dentist_id BIGINT NOT NULL REFERENCES clinic_users(id),
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    expires_at TIMESTAMP NOT NULL,
    reviewed_by_id BIGINT REFERENCES clinic_users(id),
    reviewed_at TIMESTAMP,
    appointment_id BIGINT UNIQUE REFERENCES appointments(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_request_times CHECK (end_time > start_time)
);

CREATE INDEX idx_requests_status_created ON appointment_requests (status, created_at DESC);
CREATE INDEX idx_requests_dentist_date ON appointment_requests (dentist_id, appointment_date, start_time);
CREATE INDEX idx_requests_phone ON appointment_requests (phone, status);

ALTER TABLE appointment_requests
    ADD CONSTRAINT no_overlapping_pending_requests
    EXCLUDE USING gist (
        dentist_id WITH =,
        tsrange(appointment_date + start_time, appointment_date + end_time, '[)') WITH &&
    )
    WHERE (status = 'PENDING');

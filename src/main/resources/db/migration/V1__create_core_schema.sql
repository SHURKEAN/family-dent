CREATE TABLE clinic_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'DENTIST', 'RECEPTIONIST')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    address VARCHAR(500),
    notes TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_patients_name ON patients (last_name, first_name);
CREATE INDEX idx_patients_phone ON patients (phone);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    dentist_id BIGINT NOT NULL REFERENCES clinic_users(id),
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_appointment_times CHECK (end_time > start_time)
);

CREATE INDEX idx_appointments_date ON appointments (appointment_date, start_time);
CREATE INDEX idx_appointments_patient ON appointments (patient_id, appointment_date);
CREATE INDEX idx_appointments_dentist ON appointments (dentist_id, appointment_date);

CREATE TABLE visit_notes (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    dentist_id BIGINT NOT NULL REFERENCES clinic_users(id),
    procedures_performed TEXT NOT NULL,
    observations TEXT,
    treatment TEXT,
    recommendations TEXT,
    follow_up_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

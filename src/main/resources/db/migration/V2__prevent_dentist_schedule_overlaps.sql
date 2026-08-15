CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments
    ADD CONSTRAINT no_overlapping_scheduled_appointments
    EXCLUDE USING gist (
        dentist_id WITH =,
        tsrange(appointment_date + start_time, appointment_date + end_time, '[)') WITH &&
    )
    WHERE (status = 'SCHEDULED');

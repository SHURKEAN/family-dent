package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @EntityGraph(attributePaths = {"patient", "dentist", "visitNote"})
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"patient", "dentist", "visitNote"})
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
            LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"patient", "dentist", "visitNote"})
    List<Appointment> findByAppointmentDateOrderByStartTimeAsc(LocalDate date);

    @EntityGraph(attributePaths = {"patient", "dentist", "visitNote"})
    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    @Query("""
            SELECT (COUNT(a) > 0) FROM Appointment a
            WHERE a.dentist.id = :dentistId
              AND a.appointmentDate = :date
              AND a.status = :status
              AND a.id <> :excludeId
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    boolean hasOverlap(@Param("dentistId") Long dentistId,
                       @Param("date") LocalDate date,
                       @Param("startTime") LocalTime startTime,
                       @Param("endTime") LocalTime endTime,
                       @Param("excludeId") Long excludeId,
                       @Param("status") AppointmentStatus status);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);
}

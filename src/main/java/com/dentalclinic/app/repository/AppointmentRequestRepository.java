package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.AppointmentRequest;
import com.dentalclinic.app.domain.AppointmentRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest, Long> {
    @EntityGraph(attributePaths = {"dentist", "reviewedBy", "appointment", "appointment.patient"})
    List<AppointmentRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"dentist", "reviewedBy", "appointment", "appointment.patient"})
    List<AppointmentRequest> findByStatusOrderByCreatedAtDesc(AppointmentRequestStatus status);

    @EntityGraph(attributePaths = {"dentist", "reviewedBy", "appointment", "appointment.patient"})
    @Query("SELECT r FROM AppointmentRequest r WHERE r.id = :id")
    Optional<AppointmentRequest> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AppointmentRequest r WHERE r.id = :id")
    Optional<AppointmentRequest> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT (COUNT(r) > 0) FROM AppointmentRequest r
            WHERE r.dentist.id = :dentistId
              AND r.appointmentDate = :date
              AND r.status = :status
              AND r.expiresAt > :now
              AND r.id <> :excludeId
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    boolean hasActiveOverlap(@Param("dentistId") Long dentistId,
                             @Param("date") LocalDate date,
                             @Param("startTime") LocalTime startTime,
                             @Param("endTime") LocalTime endTime,
                             @Param("excludeId") Long excludeId,
                             @Param("status") AppointmentRequestStatus status,
                             @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(r) FROM AppointmentRequest r
            WHERE r.phone = :phone AND r.status = :status AND r.expiresAt > :now
            """)
    long countActiveByPhone(@Param("phone") String phone,
                            @Param("status") AppointmentRequestStatus status,
                            @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(r) FROM AppointmentRequest r
            WHERE r.status = :status AND r.expiresAt > :now
            """)
    long countActiveByStatus(@Param("status") AppointmentRequestStatus status,
                             @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AppointmentRequest r SET r.status = :expired, r.reviewedAt = :now
            WHERE r.status = :pending AND r.expiresAt <= :now
            """)
    int expireStale(@Param("pending") AppointmentRequestStatus pending,
                    @Param("expired") AppointmentRequestStatus expired,
                    @Param("now") LocalDateTime now);
}

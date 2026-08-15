package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.SecurityRateLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SecurityRateLimitRepository extends JpaRepository<SecurityRateLimit, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SecurityRateLimit r WHERE r.bucketKey = :key")
    Optional<SecurityRateLimit> findForUpdate(@Param("key") String key);

    long deleteByUpdatedAtBefore(LocalDateTime cutoff);
}

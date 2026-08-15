package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop250ByOrderByOccurredAtDesc();
    long deleteByOccurredAtBefore(LocalDateTime cutoff);
}

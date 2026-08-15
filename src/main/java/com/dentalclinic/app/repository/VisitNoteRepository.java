package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.VisitNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitNoteRepository extends JpaRepository<VisitNote, Long> {
    @EntityGraph(attributePaths = {"appointment", "dentist"})
    Optional<VisitNote> findByAppointmentId(Long appointmentId);
}

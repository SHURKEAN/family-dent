package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("""
            SELECT p FROM Patient p
            WHERE (:includeArchived = true OR p.archived = false)
              AND (:search = '' OR
                   LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.lastName, p.firstName
            """)
    List<Patient> search(@Param("search") String search, @Param("includeArchived") boolean includeArchived);

    long countByArchivedFalse();
    List<Patient> findByArchivedFalseOrderByLastNameAscFirstNameAsc();
    Optional<Patient> findFirstByPhoneOrderByIdAsc(String phone);
}

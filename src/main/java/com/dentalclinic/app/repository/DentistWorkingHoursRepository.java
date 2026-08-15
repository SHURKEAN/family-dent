package com.dentalclinic.app.repository;

import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.WorkingDay;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DentistWorkingHoursRepository extends JpaRepository<DentistWorkingHours, Long> {
    @EntityGraph(attributePaths = "dentist")
    List<DentistWorkingHours> findAllByOrderByDentistLastNameAscDentistFirstNameAscDayOfWeekAsc();

    Optional<DentistWorkingHours> findByDentistIdAndDayOfWeek(Long dentistId, WorkingDay dayOfWeek);

    Optional<DentistWorkingHours> findByDentistIdAndDayOfWeekAndActiveTrue(Long dentistId, WorkingDay dayOfWeek);
}

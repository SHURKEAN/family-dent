package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.domain.WorkingDay;
import com.dentalclinic.app.repository.DentistWorkingHoursRepository;
import com.dentalclinic.app.web.form.WorkingHoursForm;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class WorkingHoursService {
    private final DentistWorkingHoursRepository repository;
    private final UserService userService;

    public WorkingHoursService(DentistWorkingHoursRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public List<DentistWorkingHours> all() {
        List<DentistWorkingHours> hours = repository.findAllByOrderByDentistLastNameAscDentistFirstNameAscDayOfWeekAsc();
        hours.sort(Comparator
                .comparing((DentistWorkingHours item) -> item.getDentist().getLastName())
                .thenComparing(item -> item.getDentist().getFirstName())
                .thenComparingInt(item -> item.getDayOfWeek().ordinal()));
        return hours;
    }

    public DentistWorkingHours get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClinicNotFoundException("Çalışma saati bulunamadı"));
    }

    public Optional<DentistWorkingHours> activeFor(Long dentistId, LocalDate date) {
        return repository.findByDentistIdAndDayOfWeekAndActiveTrue(dentistId, WorkingDay.from(date.getDayOfWeek()));
    }

    public WorkingHoursForm toForm(DentistWorkingHours hours) {
        WorkingHoursForm form = new WorkingHoursForm();
        form.setDentistId(hours.getDentist().getId());
        form.setDayOfWeek(hours.getDayOfWeek());
        form.setStartTime(hours.getStartTime());
        form.setEndTime(hours.getEndTime());
        form.setSlotDurationMinutes(hours.getSlotDurationMinutes());
        form.setActive(hours.isActive());
        return form;
    }

    @Transactional
    public DentistWorkingHours create(WorkingHoursForm form) {
        if (repository.findByDentistIdAndDayOfWeek(form.getDentistId(), form.getDayOfWeek()).isPresent()) {
            throw new BusinessRuleException("Bu diş hekimi için seçilen gün zaten tanımlı");
        }
        return save(new DentistWorkingHours(), form);
    }

    @Transactional
    public DentistWorkingHours update(Long id, WorkingHoursForm form) {
        DentistWorkingHours hours = get(id);
        repository.findByDentistIdAndDayOfWeek(form.getDentistId(), form.getDayOfWeek())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BusinessRuleException("Bu diş hekimi için seçilen gün zaten tanımlı"); });
        return save(hours, form);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    public DentistWorkingHours requireWithinSchedule(Long dentistId, LocalDate date,
                                                     LocalTime startTime, LocalTime endTime) {
        DentistWorkingHours hours = activeFor(dentistId, date)
                .orElseThrow(() -> new BusinessRuleException("Diş hekimi seçilen gün çalışmıyor"));
        if (startTime.isBefore(hours.getStartTime()) || endTime.isAfter(hours.getEndTime())) {
            throw new BusinessRuleException("Randevu diş hekiminin çalışma saatleri içinde olmalıdır");
        }
        return hours;
    }

    public DentistWorkingHours requireExactPublicSlot(Long dentistId, LocalDate date, LocalTime startTime) {
        DentistWorkingHours hours = activeFor(dentistId, date)
                .orElseThrow(() -> new BusinessRuleException("Diş hekimi seçilen gün çalışmıyor"));
        long offset = ChronoUnit.MINUTES.between(hours.getStartTime(), startTime);
        LocalTime endTime = startTime.plusMinutes(hours.getSlotDurationMinutes());
        if (offset < 0 || offset % hours.getSlotDurationMinutes() != 0 || endTime.isAfter(hours.getEndTime())) {
            throw new BusinessRuleException("Geçerli bir randevu saati seçin");
        }
        return hours;
    }

    private DentistWorkingHours save(DentistWorkingHours hours, WorkingHoursForm form) {
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new BusinessRuleException("Bitiş saati başlangıç saatinden sonra olmalıdır");
        }
        User dentist = userService.get(form.getDentistId());
        if (dentist.getRole() != Role.DENTIST || !dentist.isActive()) {
            throw new BusinessRuleException("Aktif bir diş hekimi seçin");
        }
        hours.setDentist(dentist);
        hours.setDayOfWeek(form.getDayOfWeek());
        hours.setStartTime(form.getStartTime());
        hours.setEndTime(form.getEndTime());
        hours.setSlotDurationMinutes(form.getSlotDurationMinutes());
        hours.setActive(form.isActive());
        try {
            return repository.saveAndFlush(hours);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("Bu çalışma saati başka bir kayıtla çakışıyor");
        }
    }
}

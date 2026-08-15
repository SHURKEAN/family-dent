package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentStatus;
import com.dentalclinic.app.domain.AppointmentRequest;
import com.dentalclinic.app.domain.AppointmentRequestStatus;
import com.dentalclinic.app.domain.Patient;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.repository.AppointmentRepository;
import com.dentalclinic.app.repository.AppointmentRequestRepository;
import com.dentalclinic.app.web.form.AppointmentForm;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientService patientService;
    private final UserService userService;
    private final WorkingHoursService workingHoursService;
    private final AppointmentRequestRepository appointmentRequestRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientService patientService,
                              UserService userService,
                              WorkingHoursService workingHoursService,
                              AppointmentRequestRepository appointmentRequestRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientService = patientService;
        this.userService = userService;
        this.workingHoursService = workingHoursService;
        this.appointmentRequestRepository = appointmentRequestRepository;
    }

    public Appointment get(Long id) {
        return appointmentRepository.findDetailedById(id)
                .orElseThrow(() -> new ClinicNotFoundException("Randevu bulunamadı"));
    }

    public List<Appointment> between(LocalDate from, LocalDate to) {
        return appointmentRepository.findByAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(from, to);
    }

    public List<Appointment> on(LocalDate date) {
        return appointmentRepository.findByAppointmentDateOrderByStartTimeAsc(date);
    }

    public List<Appointment> patientHistory(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId);
    }

    public AppointmentForm toForm(Appointment appointment) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(appointment.getPatient().getId());
        form.setDentistId(appointment.getDentist().getId());
        form.setAppointmentDate(appointment.getAppointmentDate());
        form.setStartTime(appointment.getStartTime());
        form.setEndTime(appointment.getEndTime());
        form.setReason(appointment.getReason());
        return form;
    }

    @Transactional
    public Appointment create(AppointmentForm form) {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        populateAndValidate(appointment, form, -1L);
        return saveWithConflictTranslation(appointment);
    }

    @Transactional
    public Appointment createFromRequest(AppointmentRequest request, Patient patient) {
        User dentist = userService.getForUpdate(request.getDentist().getId());
        workingHoursService.requireWithinSchedule(dentist.getId(), request.getAppointmentDate(),
                request.getStartTime(), request.getEndTime());
        if (appointmentRepository.hasOverlap(dentist.getId(), request.getAppointmentDate(),
                request.getStartTime(), request.getEndTime(), -1L, AppointmentStatus.SCHEDULED) ||
                appointmentRequestRepository.hasActiveOverlap(dentist.getId(), request.getAppointmentDate(),
                        request.getStartTime(), request.getEndTime(), request.getId(),
                        AppointmentRequestStatus.PENDING, LocalDateTime.now())) {
            throw new SchedulingConflictException("Bu saat artık uygun değil");
        }
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setReason(request.getReason());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return saveWithConflictTranslation(appointment);
    }

    @Transactional
    public Appointment update(Long id, AppointmentForm form) {
        Appointment appointment = get(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Yalnızca planlanmış randevular yeniden düzenlenebilir");
        }
        populateAndValidate(appointment, form, id);
        return saveWithConflictTranslation(appointment);
    }

    @Transactional
    public void changeStatus(Long id, AppointmentStatus newStatus, String currentUsername) {
        Appointment appointment = get(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Bu randevu zaten kapatılmış");
        }
        if (newStatus == AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("Randevu için son bir durum seçin");
        }
        User actor = userService.getByUsername(currentUsername);
        if (actor.getRole() == Role.DENTIST &&
                (!actor.getId().equals(appointment.getDentist().getId()) ||
                 (newStatus != AppointmentStatus.COMPLETED && newStatus != AppointmentStatus.NO_SHOW))) {
            throw new BusinessRuleException("Diş hekimleri yalnızca kendi randevularını tamamlandı veya gelmedi olarak işaretleyebilir");
        }
        appointment.setStatus(newStatus);
    }

    public long countScheduledOn(LocalDate date) {
        return appointmentRepository.countByAppointmentDateAndStatus(date, AppointmentStatus.SCHEDULED);
    }

    private void populateAndValidate(Appointment appointment, AppointmentForm form, Long excludeId) {
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new BusinessRuleException("Bitiş saati başlangıç saatinden sonra olmalıdır");
        }
        if (LocalDateTime.of(form.getAppointmentDate(), form.getStartTime()).isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Geçmiş bir tarihe randevu oluşturulamaz");
        }
        Patient patient = patientService.get(form.getPatientId());
        if (patient.isArchived()) {
            throw new BusinessRuleException("Arşivlenmiş hastalar için yeni randevu oluşturulamaz");
        }
        User dentist = userService.getForUpdate(form.getDentistId());
        if (dentist.getRole() != Role.DENTIST || !dentist.isActive()) {
            throw new BusinessRuleException("Aktif bir diş hekimi seçin");
        }
        workingHoursService.requireWithinSchedule(dentist.getId(), form.getAppointmentDate(),
                form.getStartTime(), form.getEndTime());
        if (appointmentRepository.hasOverlap(dentist.getId(), form.getAppointmentDate(),
                form.getStartTime(), form.getEndTime(), excludeId, AppointmentStatus.SCHEDULED)) {
            throw new SchedulingConflictException("Diş hekiminin bu saat aralığında başka bir randevusu var");
        }
        if (appointmentRequestRepository.hasActiveOverlap(dentist.getId(), form.getAppointmentDate(),
                form.getStartTime(), form.getEndTime(), -1L, AppointmentRequestStatus.PENDING, LocalDateTime.now())) {
            throw new SchedulingConflictException("Bu saat bekleyen bir hasta talebi tarafından ayrılmış");
        }
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setStartTime(form.getStartTime());
        appointment.setEndTime(form.getEndTime());
        appointment.setReason(form.getReason().trim());
    }

    private Appointment saveWithConflictTranslation(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw new SchedulingConflictException("Diş hekiminin bu saat aralığında başka bir randevusu var");
        }
    }
}

package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentRequest;
import com.dentalclinic.app.domain.AppointmentRequestStatus;
import com.dentalclinic.app.domain.AppointmentStatus;
import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.Patient;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.repository.AppointmentRepository;
import com.dentalclinic.app.repository.AppointmentRequestRepository;
import com.dentalclinic.app.web.form.PublicBookingForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AppointmentRequestService {
    private final AppointmentRequestRepository requestRepository;
    private final AppointmentRepository appointmentRepository;
    private final WorkingHoursService workingHoursService;
    private final UserService userService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final int expirationHours;
    private final int bookingWindowDays;
    private final int maximumActivePerPhone;

    public AppointmentRequestService(AppointmentRequestRepository requestRepository,
                                     AppointmentRepository appointmentRepository,
                                     WorkingHoursService workingHoursService,
                                     UserService userService,
                                     PatientService patientService,
                                     AppointmentService appointmentService,
                                     @Value("${clinic.booking.request-expiration-hours:24}") int expirationHours,
                                     @Value("${clinic.booking.window-days:60}") int bookingWindowDays,
                                     @Value("${clinic.booking.maximum-active-per-phone:2}") int maximumActivePerPhone) {
        this.requestRepository = requestRepository;
        this.appointmentRepository = appointmentRepository;
        this.workingHoursService = workingHoursService;
        this.userService = userService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.expirationHours = expirationHours;
        this.bookingWindowDays = bookingWindowDays;
        this.maximumActivePerPhone = maximumActivePerPhone;
    }

    public List<AppointmentRequest> list(AppointmentRequestStatus status) {
        expireStale();
        return status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public AppointmentRequest get(Long id) {
        expireStale();
        return requestRepository.findDetailedById(id)
                .orElseThrow(() -> new ClinicNotFoundException("Randevu talebi bulunamadı"));
    }

    public long pendingCount() {
        expireStale();
        return requestRepository.countActiveByStatus(AppointmentRequestStatus.PENDING, LocalDateTime.now());
    }

    public LocalDate maximumBookingDate() {
        return LocalDate.now().plusDays(bookingWindowDays);
    }

    public List<AvailableSlot> availableSlots(Long dentistId, LocalDate date) {
        expireStale();
        if (dentistId == null || date == null || date.isBefore(LocalDate.now()) ||
                date.isAfter(LocalDate.now().plusDays(bookingWindowDays))) {
            return List.of();
        }
        User dentist;
        try {
            dentist = userService.get(dentistId);
        } catch (ClinicNotFoundException exception) {
            return List.of();
        }
        if (dentist.getRole() != Role.DENTIST || !dentist.isActive()) {
            return List.of();
        }
        DentistWorkingHours hours = workingHoursService.activeFor(dentistId, date).orElse(null);
        if (hours == null) {
            return List.of();
        }

        List<AvailableSlot> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (LocalTime start = hours.getStartTime();
             !start.plusMinutes(hours.getSlotDurationMinutes()).isAfter(hours.getEndTime());
             start = start.plusMinutes(hours.getSlotDurationMinutes())) {
            LocalTime end = start.plusMinutes(hours.getSlotDurationMinutes());
            if (!LocalDateTime.of(date, start).isAfter(now)) {
                continue;
            }
            if (!hasConflict(dentistId, date, start, end, -1L)) {
                slots.add(new AvailableSlot(start, end));
            }
        }
        return slots;
    }

    public AppointmentRequest create(PublicBookingForm form) {
        expireStale();
        validateDate(form.getAppointmentDate(), form.getStartTime());
        String phone = normalizePhone(form.getPhone());
        int digitCount = phone.startsWith("+") ? phone.length() - 1 : phone.length();
        if (digitCount < 7 || digitCount > 15) {
            throw new BusinessRuleException("Geçerli bir telefon numarası girin");
        }
        if (requestRepository.countActiveByPhone(phone, AppointmentRequestStatus.PENDING, LocalDateTime.now())
                >= maximumActivePerPhone) {
            throw new BusinessRuleException("Bu telefon numarası için çok fazla bekleyen talep var");
        }

        User dentist;
        try {
            dentist = userService.getForUpdate(form.getDentistId());
        } catch (ClinicNotFoundException exception) {
            throw new BusinessRuleException("Aktif bir diş hekimi seçin");
        }
        if (dentist.getRole() != Role.DENTIST || !dentist.isActive()) {
            throw new BusinessRuleException("Aktif bir diş hekimi seçin");
        }
        DentistWorkingHours hours = workingHoursService.requireExactPublicSlot(
                dentist.getId(), form.getAppointmentDate(), form.getStartTime());
        LocalTime endTime = form.getStartTime().plusMinutes(hours.getSlotDurationMinutes());
        if (hasConflict(dentist.getId(), form.getAppointmentDate(), form.getStartTime(), endTime, -1L)) {
            throw new SchedulingConflictException("Bu randevu saati az önce doldu. Lütfen başka bir saat seçin");
        }

        AppointmentRequest request = new AppointmentRequest();
        request.setFirstName(form.getFirstName().trim());
        request.setLastName(form.getLastName().trim());
        request.setPhone(phone);
        request.setReason(form.getReason().trim());
        request.setDentist(dentist);
        request.setAppointmentDate(form.getAppointmentDate());
        request.setStartTime(form.getStartTime());
        request.setEndTime(endTime);
        request.setStatus(AppointmentRequestStatus.PENDING);
        request.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        try {
            return requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new SchedulingConflictException("Bu randevu saati az önce doldu. Lütfen başka bir saat seçin");
        }
    }

    public Appointment approve(Long id, String username) {
        expireStale();
        AppointmentRequest request = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ClinicNotFoundException("Randevu talebi bulunamadı"));
        requirePending(request);
        userService.getForUpdate(request.getDentist().getId());
        User reviewer = userService.getByUsername(username);
        Patient patient = patientService.resolveForPublicBooking(
                request.getFirstName(), request.getLastName(), request.getPhone());
        Appointment appointment = appointmentService.createFromRequest(request, patient);
        request.setStatus(AppointmentRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setAppointment(appointment);
        requestRepository.flush();
        return appointment;
    }

    public void reject(Long id, String username) {
        expireStale();
        AppointmentRequest request = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ClinicNotFoundException("Randevu talebi bulunamadı"));
        requirePending(request);
        request.setStatus(AppointmentRequestStatus.REJECTED);
        request.setReviewedBy(userService.getByUsername(username));
        request.setReviewedAt(LocalDateTime.now());
    }

    private void requirePending(AppointmentRequest request) {
        if (request.getStatus() != AppointmentRequestStatus.PENDING ||
                !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Yalnızca bekleyen talepler işleme alınabilir");
        }
    }

    private void validateDate(LocalDate date, LocalTime startTime) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || !LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Geçmiş bir saate randevu talebi oluşturulamaz");
        }
        if (date.isAfter(today.plusDays(bookingWindowDays))) {
            throw new BusinessRuleException("En fazla " + bookingWindowDays + " gün sonrası için randevu alınabilir");
        }
    }

    private boolean hasConflict(Long dentistId, LocalDate date, LocalTime start, LocalTime end, Long excludedRequestId) {
        return appointmentRepository.hasOverlap(dentistId, date, start, end, -1L, AppointmentStatus.SCHEDULED)
                || requestRepository.hasActiveOverlap(dentistId, date, start, end, excludedRequestId,
                        AppointmentRequestStatus.PENDING, LocalDateTime.now());
    }

    private String normalizePhone(String phone) {
        String trimmed = phone.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        return trimmed.startsWith("+") ? "+" + digits : digits;
    }

    private void expireStale() {
        LocalDateTime now = LocalDateTime.now();
        requestRepository.expireStale(AppointmentRequestStatus.PENDING, AppointmentRequestStatus.EXPIRED, now);
    }

    public record AvailableSlot(LocalTime startTime, LocalTime endTime) {}
}

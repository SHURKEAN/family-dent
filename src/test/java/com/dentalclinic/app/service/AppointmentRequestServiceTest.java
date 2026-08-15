package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentRequest;
import com.dentalclinic.app.domain.AppointmentRequestStatus;
import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.domain.WorkingDay;
import com.dentalclinic.app.repository.DentistWorkingHoursRepository;
import com.dentalclinic.app.repository.UserRepository;
import com.dentalclinic.app.web.form.PublicBookingForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentRequestServiceTest {
    @Autowired private AppointmentRequestService requestService;
    @Autowired private UserRepository userRepository;
    @Autowired private DentistWorkingHoursRepository workingHoursRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User dentist;
    private User receptionist;
    private LocalDate appointmentDate;

    @BeforeEach
    void setUp() {
        dentist = user("dr.booking", "Deniz", "Yılmaz", Role.DENTIST);
        receptionist = user("reception.booking", "Ece", "Demir", Role.RECEPTIONIST);
        appointmentDate = LocalDate.now().plusDays(8);

        DentistWorkingHours hours = new DentistWorkingHours();
        hours.setDentist(dentist);
        hours.setDayOfWeek(WorkingDay.from(appointmentDate.getDayOfWeek()));
        hours.setStartTime(LocalTime.of(9, 0));
        hours.setEndTime(LocalTime.of(12, 0));
        hours.setSlotDurationMinutes(30);
        hours.setActive(true);
        workingHoursRepository.save(hours);
    }

    @Test
    void pendingRequestImmediatelyBlocksTheSameSlot() {
        requestService.create(form("+994501111111", LocalTime.of(9, 0)));

        assertThatThrownBy(() -> requestService.create(form("+994502222222", LocalTime.of(9, 0))))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessageContaining("doldu");
    }

    @Test
    void approvalCreatesPatientAndScheduledAppointment() {
        AppointmentRequest request = requestService.create(form("+994503333333", LocalTime.of(9, 30)));

        Appointment appointment = requestService.approve(request.getId(), receptionist.getUsername());
        AppointmentRequest approved = requestService.get(request.getId());

        assertThat(approved.getStatus()).isEqualTo(AppointmentRequestStatus.APPROVED);
        assertThat(approved.getAppointment().getId()).isEqualTo(appointment.getId());
        assertThat(appointment.getPatient().getPhone()).isEqualTo("+994503333333");
        assertThat(appointment.getPatient().getDateOfBirth()).isNull();
    }

    @Test
    void onlyConfiguredFreeSlotsAreOffered() {
        requestService.create(form("+994504444444", LocalTime.of(10, 0)));

        assertThat(requestService.availableSlots(dentist.getId(), appointmentDate))
                .extracting(AppointmentRequestService.AvailableSlot::startTime)
                .contains(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(11, 0))
                .doesNotContain(LocalTime.of(10, 0));
    }

    private User user(String username, String firstName, String lastName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private PublicBookingForm form(String phone, LocalTime startTime) {
        PublicBookingForm form = new PublicBookingForm();
        form.setFirstName("Aylin");
        form.setLastName("Kaya");
        form.setPhone(phone);
        form.setReason("Kontrol");
        form.setDentistId(dentist.getId());
        form.setAppointmentDate(appointmentDate);
        form.setStartTime(startTime);
        return form;
    }
}

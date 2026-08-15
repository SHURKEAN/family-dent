package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.Patient;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.domain.WorkingDay;
import com.dentalclinic.app.repository.DentistWorkingHoursRepository;
import com.dentalclinic.app.repository.PatientRepository;
import com.dentalclinic.app.repository.UserRepository;
import com.dentalclinic.app.web.form.AppointmentForm;
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
class AppointmentServiceTest {
    @Autowired private AppointmentService appointmentService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DentistWorkingHoursRepository workingHoursRepository;
    @Autowired private AppointmentRequestService appointmentRequestService;

    private Patient patient;
    private User dentist;
    private LocalDate appointmentDate;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setFirstName("Aylin");
        patient.setLastName("Mammadova");
        patient.setDateOfBirth(LocalDate.of(1990, 3, 12));
        patient.setPhone("+994501234567");
        patient = patientRepository.save(patient);

        dentist = new User();
        dentist.setUsername("dr.test");
        dentist.setPasswordHash(passwordEncoder.encode("password123"));
        dentist.setFirstName("Leyla");
        dentist.setLastName("Aliyeva");
        dentist.setRole(Role.DENTIST);
        dentist.setActive(true);
        dentist = userRepository.save(dentist);

        appointmentDate = LocalDate.now().plusDays(7);

        DentistWorkingHours hours = new DentistWorkingHours();
        hours.setDentist(dentist);
        hours.setDayOfWeek(WorkingDay.from(appointmentDate.getDayOfWeek()));
        hours.setStartTime(LocalTime.of(8, 0));
        hours.setEndTime(LocalTime.of(18, 0));
        hours.setSlotDurationMinutes(30);
        hours.setActive(true);
        workingHoursRepository.save(hours);
    }

    @Test
    void preventsOverlappingAppointmentsForSameDentist() {
        appointmentService.create(form(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        assertThatThrownBy(() ->
                appointmentService.create(form(LocalTime.of(9, 30), LocalTime.of(10, 30))))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessageContaining("başka bir randevusu var");
    }

    @Test
    void allowsBackToBackAppointments() {
        Appointment first = appointmentService.create(form(LocalTime.of(9, 0), LocalTime.of(10, 0)));
        Appointment second = appointmentService.create(form(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThatThrownBy(() ->
                appointmentService.create(form(LocalTime.of(10, 0), LocalTime.of(9, 30))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Bitiş saati");
    }

    @Test
    void pendingPublicRequestBlocksManualAppointmentCreation() {
        PublicBookingForm requestForm = new PublicBookingForm();
        requestForm.setFirstName("Nigar");
        requestForm.setLastName("Hasanova");
        requestForm.setPhone("+994505555555");
        requestForm.setReason("Diş ağrısı");
        requestForm.setDentistId(dentist.getId());
        requestForm.setAppointmentDate(appointmentDate);
        requestForm.setStartTime(LocalTime.of(11, 0));
        appointmentRequestService.create(requestForm);

        assertThatThrownBy(() -> appointmentService.create(form(LocalTime.of(11, 0), LocalTime.of(11, 30))))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessageContaining("bekleyen bir hasta talebi");
    }

    private AppointmentForm form(LocalTime start, LocalTime end) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patient.getId());
        form.setDentistId(dentist.getId());
        form.setAppointmentDate(appointmentDate);
        form.setStartTime(start);
        form.setEndTime(end);
        form.setReason("Routine check-up");
        return form;
    }
}

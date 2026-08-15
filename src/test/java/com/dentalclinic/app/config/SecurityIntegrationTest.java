package com.dentalclinic.app.config;

import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.domain.WorkingDay;
import com.dentalclinic.app.repository.DentistWorkingHoursRepository;
import com.dentalclinic.app.repository.AppointmentRequestRepository;
import com.dentalclinic.app.repository.AuditEventRepository;
import com.dentalclinic.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DentistWorkingHoursRepository workingHoursRepository;
    @Autowired private AppointmentRequestRepository appointmentRequestRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void redirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousPatientCanOpenPublicBookingPage() throws Exception {
        mockMvc.perform(get("/randevu-al"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Online randevu")))
                .andExpect(content().string(containsString("Hesap açmadan randevu talebinizi gönderin")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }

    @Test
    @Transactional
    void publicBookingFormRendersForConfiguredWorkingDay() throws Exception {
        User dentist = new User();
        dentist.setUsername("public.form.dentist");
        dentist.setPasswordHash("not-used-in-test");
        dentist.setFirstName("Selin");
        dentist.setLastName("Aras");
        dentist.setRole(Role.DENTIST);
        dentist.setActive(true);
        dentist = userRepository.save(dentist);

        LocalDate date = LocalDate.now().plusDays(3);
        DentistWorkingHours hours = new DentistWorkingHours();
        hours.setDentist(dentist);
        hours.setDayOfWeek(WorkingDay.from(date.getDayOfWeek()));
        hours.setStartTime(LocalTime.of(9, 0));
        hours.setEndTime(LocalTime.of(11, 0));
        hours.setSlotDurationMinutes(30);
        hours.setActive(true);
        workingHoursRepository.save(hours);

        mockMvc.perform(get("/randevu-al")
                        .param("dentistId", dentist.getId().toString())
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Randevu talebini gönder")))
                .andExpect(content().string(containsString("Saat seçimi tek başına talep oluşturmaz")))
                .andExpect(content().string(containsString("09:00")));

        mockMvc.perform(post("/randevu-al")
                        .with(csrf())
                        .param("firstName", "Aylin")
                        .param("lastName", "Kaya")
                        .param("phone", "+994 50 123 45 67")
                        .param("reason", "Kontrol")
                        .param("dentistId", dentist.getId().toString())
                        .param("appointmentDate", date.toString())
                        .param("startTime", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/randevu-al/tamamlandi"))
                .andExpect(flash().attributeExists("referenceCode"));

        assertThat(appointmentRequestRepository.count()).isEqualTo(1);
    }

    @Test
    void successPageCannotBeShownWithoutASavedRequestReference() throws Exception {
        mockMvc.perform(get("/randevu-al/tamamlandi"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/randevu-al"));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCanViewPatients() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistPagesAndFormsRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<!doctype html>")))
                .andExpect(content().string(containsString("<html lang=\"tr\"")))
                .andExpect(content().string(containsString("Klinik paneli")))
                .andExpect(content().string(containsString("/webjars/bootstrap/5.3.8/css/bootstrap.min.css")))
                .andExpect(content().string(containsString("/css/app.css")));
        mockMvc.perform(get("/schedule"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/appointments/new"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/patients/new"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/appointment-requests"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/working-hours"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorPagesRender() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit-events"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Denetim kayıtları")));
    }

    @Test
    @Transactional
    void repeatedLoginFailuresAreBlockedAndAudited() throws Exception {
        User user = new User();
        user.setUsername("throttle.test");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword123!"));
        user.setFirstName("Login");
        user.setLastName("Test");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        userRepository.saveAndFlush(user);

        for (int attempt = 1; attempt < 5; attempt++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .with(request -> { request.setRemoteAddr("203.0.113.25"); return request; })
                            .param("username", "throttle.test")
                            .param("password", "wrong-password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(request -> { request.setRemoteAddr("203.0.113.25"); return request; })
                        .param("username", "throttle.test")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?blocked"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(request -> { request.setRemoteAddr("203.0.113.25"); return request; })
                        .param("username", "throttle.test")
                        .param("password", "CorrectPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?blocked"));

        assertThat(auditEventRepository.findTop250ByOrderByOccurredAtDesc())
                .anyMatch(event -> event.getEventType().equals("LOGIN_BLOCKED"));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCannotManageEmployees() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DENTIST")
    void dentistCannotOpenPatientEditForm() throws Exception {
        mockMvc.perform(get("/patients/1/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DENTIST")
    void dentistCannotManageWorkingHoursOrPublicRequests() throws Exception {
        mockMvc.perform(get("/working-hours"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/appointment-requests"))
                .andExpect(status().isForbidden());
    }
}

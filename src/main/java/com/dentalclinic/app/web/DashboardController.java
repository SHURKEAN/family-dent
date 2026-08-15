package com.dentalclinic.app.web;

import com.dentalclinic.app.service.AppointmentService;
import com.dentalclinic.app.service.AppointmentRequestService;
import com.dentalclinic.app.service.PatientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class DashboardController {
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final AppointmentRequestService appointmentRequestService;

    public DashboardController(PatientService patientService,
                               AppointmentService appointmentService,
                               AppointmentRequestService appointmentRequestService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.appointmentRequestService = appointmentRequestService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("activePatientCount", patientService.countActive());
        model.addAttribute("scheduledTodayCount", appointmentService.countScheduledOn(today));
        model.addAttribute("pendingRequestCount", appointmentRequestService.pendingCount());
        model.addAttribute("todayAppointments", appointmentService.on(today));
        return "dashboard";
    }
}

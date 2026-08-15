package com.dentalclinic.app.web;

import com.dentalclinic.app.service.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class ScheduleController {
    private final AppointmentService appointmentService;

    public ScheduleController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("today", today);
        model.addAttribute("todayAppointments", appointmentService.on(today));
        model.addAttribute("upcomingAppointments", appointmentService.between(today.plusDays(1), today.plusDays(14)));
        return "schedule";
    }
}

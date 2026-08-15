package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentStatus;
import com.dentalclinic.app.service.AppointmentService;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.PatientService;
import com.dentalclinic.app.service.SchedulingConflictException;
import com.dentalclinic.app.service.UserService;
import com.dentalclinic.app.web.form.AppointmentForm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final UserService userService;

    public AppointmentController(AppointmentService appointmentService,
                                 PatientService patientService,
                                 UserService userService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        LocalDate effectiveFrom = from == null ? LocalDate.now() : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.plusDays(30) : to;
        if (effectiveTo.isBefore(effectiveFrom)) {
            effectiveTo = effectiveFrom;
        }
        model.addAttribute("appointments", appointmentService.between(effectiveFrom, effectiveTo));
        model.addAttribute("from", effectiveFrom);
        model.addAttribute("to", effectiveTo);
        return "appointments/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long patientId, Model model) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patientId);
        form.setAppointmentDate(LocalDate.now().plusDays(1));
        form.setStartTime(LocalTime.of(9, 0));
        form.setEndTime(LocalTime.of(9, 30));
        model.addAttribute("appointmentForm", form);
        prepareForm(model, false, null);
        return "appointments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute AppointmentForm appointmentForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Appointment saved = appointmentService.create(appointmentForm);
                redirectAttributes.addFlashAttribute("success", "Randevu oluşturuldu");
                return "redirect:/patients/" + saved.getPatient().getId();
            } catch (BusinessRuleException | SchedulingConflictException exception) {
                bindingResult.reject("appointment", exception.getMessage());
            }
        }
        prepareForm(model, false, null);
        return "appointments/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.get(id);
        model.addAttribute("appointmentForm", appointmentService.toForm(appointment));
        prepareForm(model, true, appointment);
        return "appointments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute AppointmentForm appointmentForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentService.get(id);
        if (!bindingResult.hasErrors()) {
            try {
                appointmentService.update(id, appointmentForm);
                redirectAttributes.addFlashAttribute("success", "Randevu yeniden planlandı");
                return "redirect:/patients/" + appointment.getPatient().getId();
            } catch (BusinessRuleException | SchedulingConflictException exception) {
                bindingResult.reject("appointment", exception.getMessage());
            }
        }
        prepareForm(model, true, appointment);
        return "appointments/form";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam AppointmentStatus status,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            appointmentService.changeStatus(id, status, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Randevu durumu güncellendi: " + status.getDisplayName());
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/appointments";
    }

    private void prepareForm(Model model, boolean editing, Appointment appointment) {
        model.addAttribute("patients", patientService.activePatients());
        model.addAttribute("dentists", userService.activeDentists());
        model.addAttribute("editing", editing);
        model.addAttribute("appointment", appointment);
    }
}

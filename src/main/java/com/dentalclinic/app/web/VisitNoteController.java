package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.VisitNote;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.AppointmentService;
import com.dentalclinic.app.service.VisitNoteService;
import com.dentalclinic.app.web.form.VisitNoteForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VisitNoteController {
    private final VisitNoteService visitNoteService;
    private final AppointmentService appointmentService;

    public VisitNoteController(VisitNoteService visitNoteService, AppointmentService appointmentService) {
        this.visitNoteService = visitNoteService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments/{appointmentId}/visit-note")
    public String form(@PathVariable Long appointmentId, Authentication authentication, Model model) {
        Appointment appointment = appointmentService.get(appointmentId);
        visitNoteService.assertCanManage(appointment, authentication.getName());
        VisitNoteForm form = visitNoteService.findForAppointment(appointmentId)
                .map(visitNoteService::toForm).orElseGet(VisitNoteForm::new);
        model.addAttribute("appointment", appointment);
        model.addAttribute("visitNoteForm", form);
        model.addAttribute("editing", appointment.getVisitNote() != null);
        return "appointments/visit-note";
    }

    @PostMapping("/appointments/{appointmentId}/visit-note")
    public String save(@PathVariable Long appointmentId,
                       @Valid @ModelAttribute VisitNoteForm visitNoteForm,
                       BindingResult bindingResult,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentService.get(appointmentId);
        if (!bindingResult.hasErrors()) {
            try {
                VisitNote note = visitNoteService.save(appointmentId, visitNoteForm, authentication.getName());
                redirectAttributes.addFlashAttribute("success", "Ziyaret notu kaydedildi");
                return "redirect:/patients/" + note.getAppointment().getPatient().getId();
            } catch (BusinessRuleException exception) {
                bindingResult.reject("visitNote", exception.getMessage());
            }
        }
        model.addAttribute("appointment", appointment);
        model.addAttribute("editing", appointment.getVisitNote() != null);
        return "appointments/visit-note";
    }
}

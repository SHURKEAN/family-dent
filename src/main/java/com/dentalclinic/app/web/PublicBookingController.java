package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.AppointmentRequest;
import com.dentalclinic.app.service.AppointmentRequestService;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.PublicBookingRateLimiter;
import com.dentalclinic.app.service.SchedulingConflictException;
import com.dentalclinic.app.service.UserService;
import com.dentalclinic.app.web.form.PublicBookingForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class PublicBookingController {
    private final AppointmentRequestService requestService;
    private final UserService userService;
    private final PublicBookingRateLimiter rateLimiter;

    public PublicBookingController(AppointmentRequestService requestService,
                                   UserService userService,
                                   PublicBookingRateLimiter rateLimiter) {
        this.requestService = requestService;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/randevu-al")
    public String form(@RequestParam(required = false) Long dentistId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        PublicBookingForm form = new PublicBookingForm();
        var dentists = userService.activeDentists();
        Long selectedDentistId = dentists.stream()
                .filter(dentist -> dentist.getId().equals(dentistId))
                .map(dentist -> dentist.getId())
                .findFirst()
                .orElseGet(() -> dentists.stream().findFirst().map(dentist -> dentist.getId()).orElse(null));
        LocalDate selectedDate = date != null ? date : LocalDate.now().plusDays(1);
        form.setDentistId(selectedDentistId);
        form.setAppointmentDate(selectedDate);
        model.addAttribute("publicBookingForm", form);
        prepare(model, selectedDentistId, selectedDate);
        return "public-booking/form";
    }

    @PostMapping("/randevu-al")
    public String submit(@Valid @ModelAttribute PublicBookingForm publicBookingForm,
                         BindingResult bindingResult,
                         HttpServletRequest servletRequest,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                rateLimiter.check(servletRequest.getRemoteAddr());
                AppointmentRequest saved = requestService.create(publicBookingForm);
                redirectAttributes.addFlashAttribute("referenceCode", saved.getReferenceCode());
                return "redirect:/randevu-al/tamamlandi";
            } catch (BusinessRuleException | SchedulingConflictException exception) {
                bindingResult.reject("booking", exception.getMessage());
            }
        }
        prepare(model, publicBookingForm.getDentistId(), publicBookingForm.getAppointmentDate());
        return "public-booking/form";
    }

    @GetMapping("/randevu-al/tamamlandi")
    public String success(Model model) {
        if (!model.containsAttribute("referenceCode")) {
            return "redirect:/randevu-al";
        }
        return "public-booking/success";
    }

    private void prepare(Model model, Long dentistId, LocalDate date) {
        model.addAttribute("dentists", userService.activeDentists());
        model.addAttribute("slots", requestService.availableSlots(dentistId, date));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maximumDate", requestService.maximumBookingDate());
        model.addAttribute("selectedDentistId", dentistId);
        model.addAttribute("selectedDate", date);
    }
}

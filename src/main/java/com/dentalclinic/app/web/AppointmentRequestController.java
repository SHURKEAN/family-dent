package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.AppointmentRequestStatus;
import com.dentalclinic.app.service.AppointmentRequestService;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.SchedulingConflictException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/appointment-requests")
public class AppointmentRequestController {
    private final AppointmentRequestService requestService;

    public AppointmentRequestController(AppointmentRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AppointmentRequestStatus status, Model model) {
        model.addAttribute("requests", requestService.list(status));
        model.addAttribute("statuses", AppointmentRequestStatus.values());
        model.addAttribute("selectedStatus", status);
        return "appointment-requests/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            requestService.approve(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Randevu talebi onaylandı ve randevu oluşturuldu");
        } catch (BusinessRuleException | SchedulingConflictException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/appointment-requests";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            requestService.reject(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Randevu talebi reddedildi; saat yeniden kullanıma açıldı");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/appointment-requests";
    }
}

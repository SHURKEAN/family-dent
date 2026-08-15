package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.DentistWorkingHours;
import com.dentalclinic.app.domain.WorkingDay;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.UserService;
import com.dentalclinic.app.service.WorkingHoursService;
import com.dentalclinic.app.web.form.WorkingHoursForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

@Controller
@RequestMapping("/working-hours")
public class WorkingHoursController {
    private final WorkingHoursService workingHoursService;
    private final UserService userService;

    public WorkingHoursController(WorkingHoursService workingHoursService, UserService userService) {
        this.workingHoursService = workingHoursService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("workingHours", workingHoursService.all());
        return "working-hours/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        WorkingHoursForm form = new WorkingHoursForm();
        form.setStartTime(LocalTime.of(9, 0));
        form.setEndTime(LocalTime.of(18, 0));
        form.setSlotDurationMinutes(30);
        form.setActive(true);
        model.addAttribute("workingHoursForm", form);
        prepareForm(model, false, null);
        return "working-hours/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute WorkingHoursForm workingHoursForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                workingHoursService.create(workingHoursForm);
                redirectAttributes.addFlashAttribute("success", "Çalışma saati eklendi");
                return "redirect:/working-hours";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("workingHours", exception.getMessage());
            }
        }
        prepareForm(model, false, null);
        return "working-hours/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        DentistWorkingHours hours = workingHoursService.get(id);
        model.addAttribute("workingHoursForm", workingHoursService.toForm(hours));
        prepareForm(model, true, hours);
        return "working-hours/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute WorkingHoursForm workingHoursForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        DentistWorkingHours hours = workingHoursService.get(id);
        if (!bindingResult.hasErrors()) {
            try {
                workingHoursService.update(id, workingHoursForm);
                redirectAttributes.addFlashAttribute("success", "Çalışma saati güncellendi");
                return "redirect:/working-hours";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("workingHours", exception.getMessage());
            }
        }
        prepareForm(model, true, hours);
        return "working-hours/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        workingHoursService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Çalışma saati silindi");
        return "redirect:/working-hours";
    }

    private void prepareForm(Model model, boolean editing, DentistWorkingHours hours) {
        model.addAttribute("dentists", userService.activeDentists());
        model.addAttribute("days", WorkingDay.values());
        model.addAttribute("editing", editing);
        model.addAttribute("hours", hours);
    }
}

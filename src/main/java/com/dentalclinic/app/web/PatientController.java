package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.Patient;
import com.dentalclinic.app.service.AppointmentService;
import com.dentalclinic.app.service.PatientService;
import com.dentalclinic.app.web.form.PatientForm;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/patients")
public class PatientController {
    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean includeArchived,
                       Model model) {
        model.addAttribute("patients", patientService.search(q, includeArchived));
        model.addAttribute("q", q);
        model.addAttribute("includeArchived", includeArchived);
        return "patients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("patientForm", new PatientForm());
        model.addAttribute("editing", false);
        return "patients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute PatientForm patientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "patients/form";
        }
        Patient patient = patientService.create(patientForm);
        redirectAttributes.addFlashAttribute("success", "Hasta başarıyla eklendi");
        return "redirect:/patients/" + patient.getId();
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        model.addAttribute("patient", patientService.get(id));
        model.addAttribute("appointments", appointmentService.patientHistory(id));
        return "patients/profile";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Patient patient = patientService.get(id);
        model.addAttribute("patient", patient);
        model.addAttribute("patientForm", patientService.toForm(patient));
        model.addAttribute("editing", true);
        return "patients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute PatientForm patientForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("patient", patientService.get(id));
            model.addAttribute("editing", true);
            return "patients/form";
        }
        patientService.update(id, patientForm);
        redirectAttributes.addFlashAttribute("success", "Hasta bilgileri güncellendi");
        return "redirect:/patients/" + id;
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id,
                          @RequestParam boolean archived,
                          RedirectAttributes redirectAttributes) {
        patientService.setArchived(id, archived);
        redirectAttributes.addFlashAttribute("success", archived ? "Hasta arşivlendi" : "Hasta geri yüklendi");
        return "redirect:/patients/" + id;
    }
}

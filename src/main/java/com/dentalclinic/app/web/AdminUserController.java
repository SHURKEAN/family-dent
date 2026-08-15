package com.dentalclinic.app.web;

import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.service.BusinessRuleException;
import com.dentalclinic.app.service.UserService;
import com.dentalclinic.app.web.form.PasswordForm;
import com.dentalclinic.app.web.form.UserForm;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.all());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", Role.values());
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute UserForm userForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (userService.usernameExists(userForm.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Bu kullanıcı adı zaten kullanılıyor");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }
        userService.create(userForm);
        redirectAttributes.addFlashAttribute("success", "Çalışan hesabı oluşturuldu");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/active")
    public String active(@PathVariable Long id,
                         @RequestParam boolean active,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.setActive(id, active, authentication.getName());
            redirectAttributes.addFlashAttribute("success", active ? "Çalışan etkinleştirildi" : "Çalışan pasifleştirildi");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/password")
    public String passwordForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.get(id));
        model.addAttribute("passwordForm", new PasswordForm());
        return "admin/users/password";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable Long id,
                                @Valid @ModelAttribute PasswordForm passwordForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.get(id));
            return "admin/users/password";
        }
        userService.resetPassword(id, passwordForm.getPassword());
        redirectAttributes.addFlashAttribute("success", "Şifre başarıyla sıfırlandı");
        return "redirect:/admin/users";
    }
}

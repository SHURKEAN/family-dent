package com.dentalclinic.app.web;

import com.dentalclinic.app.service.AuditEventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit-events")
public class AdminAuditController {
    private final AuditEventService auditEventService;

    public AdminAuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("auditEvents", auditEventService.recent());
        return "admin/audit-events/list";
    }
}

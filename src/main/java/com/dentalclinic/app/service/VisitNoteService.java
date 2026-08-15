package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Appointment;
import com.dentalclinic.app.domain.AppointmentStatus;
import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.domain.VisitNote;
import com.dentalclinic.app.repository.VisitNoteRepository;
import com.dentalclinic.app.web.form.VisitNoteForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class VisitNoteService {
    private final VisitNoteRepository visitNoteRepository;
    private final AppointmentService appointmentService;
    private final UserService userService;

    public VisitNoteService(VisitNoteRepository visitNoteRepository,
                            AppointmentService appointmentService,
                            UserService userService) {
        this.visitNoteRepository = visitNoteRepository;
        this.appointmentService = appointmentService;
        this.userService = userService;
    }

    public Optional<VisitNote> findForAppointment(Long appointmentId) {
        return visitNoteRepository.findByAppointmentId(appointmentId);
    }

    public void assertCanManage(Appointment appointment, String username) {
        User actor = userService.getByUsername(username);
        if (actor.getRole() != Role.ADMIN && !actor.getId().equals(appointment.getDentist().getId())) {
            throw new AccessDeniedException("Bu ziyaret notuna yalnızca atanmış diş hekimi erişebilir");
        }
    }

    public VisitNoteForm toForm(VisitNote note) {
        VisitNoteForm form = new VisitNoteForm();
        form.setProceduresPerformed(note.getProceduresPerformed());
        form.setObservations(note.getObservations());
        form.setTreatment(note.getTreatment());
        form.setRecommendations(note.getRecommendations());
        form.setFollowUpRequired(note.isFollowUpRequired());
        return form;
    }

    @Transactional
    public VisitNote save(Long appointmentId, VisitNoteForm form, String currentUsername) {
        Appointment appointment = appointmentService.get(appointmentId);
        User actor = userService.getByUsername(currentUsername);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Ziyaret notları yalnızca tamamlanan randevulara eklenebilir");
        }
        assertCanManage(appointment, currentUsername);
        VisitNote note = visitNoteRepository.findByAppointmentId(appointmentId).orElseGet(VisitNote::new);
        if (note.getId() == null) {
            note.setAppointment(appointment);
            note.setDentist(appointment.getDentist());
        }
        note.setProceduresPerformed(form.getProceduresPerformed().trim());
        note.setObservations(blankToNull(form.getObservations()));
        note.setTreatment(blankToNull(form.getTreatment()));
        note.setRecommendations(blankToNull(form.getRecommendations()));
        note.setFollowUpRequired(form.isFollowUpRequired());
        return visitNoteRepository.save(note);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

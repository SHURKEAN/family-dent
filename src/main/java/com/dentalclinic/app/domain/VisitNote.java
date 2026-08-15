package com.dentalclinic.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "visit_notes")
public class VisitNote extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentist_id", nullable = false)
    private User dentist;
    @Column(name = "procedures_performed", nullable = false, columnDefinition = "TEXT")
    private String proceduresPerformed;
    @Column(columnDefinition = "TEXT")
    private String observations;
    @Column(columnDefinition = "TEXT")
    private String treatment;
    @Column(columnDefinition = "TEXT")
    private String recommendations;
    @Column(name = "follow_up_required", nullable = false)
    private boolean followUpRequired;

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public User getDentist() { return dentist; }
    public void setDentist(User dentist) { this.dentist = dentist; }
    public String getProceduresPerformed() { return proceduresPerformed; }
    public void setProceduresPerformed(String proceduresPerformed) { this.proceduresPerformed = proceduresPerformed; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public boolean isFollowUpRequired() { return followUpRequired; }
    public void setFollowUpRequired(boolean followUpRequired) { this.followUpRequired = followUpRequired; }
}

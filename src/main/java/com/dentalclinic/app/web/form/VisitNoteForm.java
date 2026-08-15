package com.dentalclinic.app.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VisitNoteForm {
    @NotBlank @Size(max = 10000)
    private String proceduresPerformed;
    @Size(max = 10000)
    private String observations;
    @Size(max = 10000)
    private String treatment;
    @Size(max = 10000)
    private String recommendations;
    private boolean followUpRequired;

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

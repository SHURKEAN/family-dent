package com.dentalclinic.app.web.form;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class PublicBookingForm {
    @NotBlank @Size(max = 80)
    private String firstName;
    @NotBlank @Size(max = 80)
    private String lastName;
    @NotBlank @Size(max = 30)
    @Pattern(regexp = "^[+0-9][0-9() +\\-]{6,29}$", message = "Geçerli bir telefon numarası girin")
    private String phone;
    @NotBlank @Size(max = 500)
    private String reason;
    @NotNull
    private Long dentistId;
    @NotNull @FutureOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getDentistId() { return dentistId; }
    public void setDentistId(Long dentistId) { this.dentistId = dentistId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
}

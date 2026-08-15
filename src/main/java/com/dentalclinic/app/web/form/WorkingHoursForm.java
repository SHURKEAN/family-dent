package com.dentalclinic.app.web.form;

import com.dentalclinic.app.domain.WorkingDay;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class WorkingHoursForm {
    @NotNull
    private Long dentistId;
    @NotNull
    private WorkingDay dayOfWeek;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;
    @Min(10) @Max(180)
    private int slotDurationMinutes = 30;
    private boolean active = true;

    public Long getDentistId() { return dentistId; }
    public void setDentistId(Long dentistId) { this.dentistId = dentistId; }
    public WorkingDay getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(WorkingDay dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public int getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(int slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

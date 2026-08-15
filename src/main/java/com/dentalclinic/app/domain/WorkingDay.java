package com.dentalclinic.app.domain;

import java.time.DayOfWeek;

public enum WorkingDay {
    MONDAY("1-ci gün", DayOfWeek.MONDAY),
    TUESDAY("2-ci gün", DayOfWeek.TUESDAY),
    WEDNESDAY("3-cü gün", DayOfWeek.WEDNESDAY),
    THURSDAY("4-cü gün", DayOfWeek.THURSDAY),
    FRIDAY("5-ci gün", DayOfWeek.FRIDAY),
    SATURDAY("6-cı gün", DayOfWeek.SATURDAY),
    SUNDAY("7-ci gün", DayOfWeek.SUNDAY);

    private final String displayName;
    private final DayOfWeek dayOfWeek;

    WorkingDay(String displayName, DayOfWeek dayOfWeek) {
        this.displayName = displayName;
        this.dayOfWeek = dayOfWeek;
    }

    public String getDisplayName() { return displayName; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }

    public static WorkingDay from(DayOfWeek dayOfWeek) {
        return valueOf(dayOfWeek.name());
    }
}

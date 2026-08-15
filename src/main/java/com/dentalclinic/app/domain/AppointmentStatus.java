package com.dentalclinic.app.domain;

public enum AppointmentStatus {
    SCHEDULED("Planlandı", "primary"),
    COMPLETED("Tamamlandı", "success"),
    CANCELLED("İptal edildi", "secondary"),
    NO_SHOW("Gelmedi", "warning");

    private final String displayName;
    private final String badgeClass;

    AppointmentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}

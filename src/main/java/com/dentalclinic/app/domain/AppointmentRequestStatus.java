package com.dentalclinic.app.domain;

public enum AppointmentRequestStatus {
    PENDING("Bekliyor", "warning"),
    APPROVED("Onaylandı", "success"),
    REJECTED("Reddedildi", "secondary"),
    EXPIRED("Süresi doldu", "light");

    private final String displayName;
    private final String badgeClass;

    AppointmentRequestStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}

package com.dentalclinic.app.domain;

public enum Role {
    ADMIN("Yönetici"),
    DENTIST("Diş Hekimi"),
    RECEPTIONIST("Resepsiyonist");

    private final String displayName;

    Role(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}

package com.dentalclinic.app.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordForm {
    @NotBlank @Size(min = 12, max = 72)
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

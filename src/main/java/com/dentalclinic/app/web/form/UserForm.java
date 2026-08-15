package com.dentalclinic.app.web.form;

import com.dentalclinic.app.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserForm {
    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Yalnızca harf, rakam, nokta, alt çizgi veya kısa çizgi kullanın")
    private String username;
    @NotBlank @Size(min = 12, max = 72)
    private String password;
    @NotBlank @Size(max = 80)
    private String firstName;
    @NotBlank @Size(max = 80)
    private String lastName;
    @NotNull
    private Role role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

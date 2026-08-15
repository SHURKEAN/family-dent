package com.dentalclinic.app.config;

import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "clinic.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminConfig implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminConfig.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${clinic.bootstrap-admin.enabled:true}") private boolean enabled;
    @Value("${clinic.bootstrap-admin.username:admin}") private String username;
    @Value("${clinic.bootstrap-admin.password:ChangeMe123!}") private String password;
    @Value("${clinic.bootstrap-admin.first-name:Sistem}") private String firstName;
    @Value("${clinic.bootstrap-admin.last-name:Yöneticisi}") private String lastName;

    public BootstrapAdminConfig(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters");
        }
        User admin = new User();
        admin.setUsername(username.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFirstName(firstName.trim());
        admin.setLastName(lastName.trim());
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.warn("Başlangıç yönetici hesabı '{}' oluşturuldu. Üretim kullanımından önce şifresini değiştirin.", admin.getUsername());
    }
}

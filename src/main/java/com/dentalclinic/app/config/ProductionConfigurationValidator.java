package com.dentalclinic.app.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final Environment environment;

    public ProductionConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireStrongSecret("DB_PASSWORD", 16);
        requireStrongSecret("FINGERPRINT_SECRET", 32);
        boolean bootstrapEnabled = environment.getProperty("clinic.bootstrap-admin.enabled", Boolean.class, true);
        if (bootstrapEnabled) {
            requireStrongSecret("BOOTSTRAP_ADMIN_PASSWORD", 12);
        }
        if (!environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)) {
            throw new IllegalStateException("Production session cookies must be secure");
        }
    }

    private void requireStrongSecret(String key, int minimumLength) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank() || value.length() < minimumLength
                || "dental".equalsIgnoreCase(value) || "ChangeMe123!".equals(value)) {
            throw new IllegalStateException(key + " is missing, known, or shorter than " + minimumLength + " characters");
        }
    }
}

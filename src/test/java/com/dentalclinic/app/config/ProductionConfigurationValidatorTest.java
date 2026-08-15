package com.dentalclinic.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProductionConfigurationValidatorTest {
    @Test
    void acceptsStrongProductionSecretsAndSecureCookies() {
        MockEnvironment environment = baseEnvironment()
                .withProperty("DB_PASSWORD", "a-strong-database-password")
                .withProperty("FINGERPRINT_SECRET", "a-strong-fingerprint-secret-with-32-characters")
                .withProperty("BOOTSTRAP_ADMIN_PASSWORD", "a-strong-admin-password");

        assertThatCode(() -> new ProductionConfigurationValidator(environment).run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsKnownDevelopmentDatabasePassword() {
        MockEnvironment environment = baseEnvironment()
                .withProperty("DB_PASSWORD", "dental")
                .withProperty("FINGERPRINT_SECRET", "a-strong-fingerprint-secret-with-32-characters")
                .withProperty("BOOTSTRAP_ADMIN_PASSWORD", "a-strong-admin-password");

        assertThatThrownBy(() -> new ProductionConfigurationValidator(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    private MockEnvironment baseEnvironment() {
        return new MockEnvironment()
                .withProperty("clinic.bootstrap-admin.enabled", "true")
                .withProperty("server.servlet.session.cookie.secure", "true");
    }
}

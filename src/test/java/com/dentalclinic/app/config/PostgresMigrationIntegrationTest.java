package com.dentalclinic.app.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresMigrationIntegrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesProductionSecurityAndSchedulingConstraints() {
        Integer securityTables = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('security_rate_limits', 'audit_events')
                """, Integer.class);
        Integer overlapConstraints = jdbcTemplate.queryForObject("""
                select count(*) from pg_constraint
                where conname in ('no_overlapping_scheduled_appointments', 'no_overlapping_pending_requests')
                """, Integer.class);
        String migration = jdbcTemplate.queryForObject("""
                select version from flyway_schema_history
                where success = true order by installed_rank desc limit 1
                """, String.class);

        assertThat(securityTables).isEqualTo(2);
        assertThat(overlapConstraints).isEqualTo(2);
        assertThat(migration).isEqualTo("4");
    }
}

package com.dentalclinic.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistentRateLimitServiceTest {
    @Autowired private PersistentRateLimitService rateLimitService;

    @Test
    void limitPersistsCountsAndBlocksAfterTheAllowance() {
        assertThat(rateLimitService.tryConsume("TEST_BOOKING", "198.51.100.10", 2, Duration.ofHours(1))).isTrue();
        assertThat(rateLimitService.tryConsume("TEST_BOOKING", "198.51.100.10", 2, Duration.ofHours(1))).isTrue();
        assertThat(rateLimitService.tryConsume("TEST_BOOKING", "198.51.100.10", 2, Duration.ofHours(1))).isFalse();
        assertThat(rateLimitService.isBlocked("TEST_BOOKING", "198.51.100.10")).isTrue();
    }

    @Test
    void resetClearsAnAuthenticationBucket() {
        rateLimitService.recordFailure("TEST_LOGIN", "user|198.51.100.20", 1,
                Duration.ofMinutes(15), Duration.ofMinutes(15));
        assertThat(rateLimitService.isBlocked("TEST_LOGIN", "user|198.51.100.20")).isTrue();

        rateLimitService.reset("TEST_LOGIN", "user|198.51.100.20");

        assertThat(rateLimitService.isBlocked("TEST_LOGIN", "user|198.51.100.20")).isFalse();
    }
}

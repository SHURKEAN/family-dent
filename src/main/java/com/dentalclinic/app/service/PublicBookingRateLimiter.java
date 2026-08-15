package com.dentalclinic.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PublicBookingRateLimiter {
    private final PersistentRateLimitService rateLimitService;
    private final int maximumRequestsPerHour;

    public PublicBookingRateLimiter(PersistentRateLimitService rateLimitService,
            @Value("${clinic.booking.maximum-requests-per-ip-per-hour:10}") int maximumRequestsPerHour) {
        this.rateLimitService = rateLimitService;
        this.maximumRequestsPerHour = maximumRequestsPerHour;
    }

    public void check(String remoteAddress) {
        if (!rateLimitService.tryConsume("PUBLIC_BOOKING_IP", remoteAddress,
                maximumRequestsPerHour, Duration.ofHours(1))) {
            throw new BusinessRuleException("Çok fazla talep gönderdiniz. Lütfen daha sonra tekrar deneyin");
        }
    }
}

package com.dentalclinic.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class SecurityFingerprintService {
    private final byte[] secret;

    public SecurityFingerprintService(@Value("${clinic.security.fingerprint-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("FINGERPRINT_SECRET must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String fingerprint(String category, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal((category + ":" + normalize(value)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate security fingerprint", exception);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase();
    }
}

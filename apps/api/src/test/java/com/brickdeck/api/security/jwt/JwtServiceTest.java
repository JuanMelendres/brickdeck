package com.brickdeck.api.security.jwt;

import com.brickdeck.api.security.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long-000";

    private JwtService serviceWithTtl(long minutes) {
        return new JwtService(new JwtProperties(SECRET, minutes));
    }

    @Test
    void generatedTokenCarriesTheUserIdAsSubject() {
        JwtService service = serviceWithTtl(120);
        UUID userId = UUID.randomUUID();

        String token = service.generateToken(userId, "user@brickdeck.test");

        assertThat(service.getSubject(token)).isEqualTo(userId.toString());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = serviceWithTtl(120);
        String token = service.generateToken(UUID.randomUUID(), "user@brickdeck.test");

        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> service.getSubject(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = serviceWithTtl(-1);
        String token = service.generateToken(UUID.randomUUID(), "user@brickdeck.test");

        assertThatThrownBy(() -> service.getSubject(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void secretShorterThanRequiredFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 120)))
                .isInstanceOf(IllegalStateException.class);
    }
}

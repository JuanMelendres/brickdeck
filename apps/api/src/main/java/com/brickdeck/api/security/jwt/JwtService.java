package com.brickdeck.api.security.jwt;

import com.brickdeck.api.security.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    /** HS256 requires a key of at least 256 bits (32 bytes). */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        byte[] secretBytes = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES
                            + " bytes. Set JWT_SECRET to a longer value.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = properties.expirationMinutes();
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}

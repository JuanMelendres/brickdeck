package com.brickdeck.api.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brickdeck.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {
}

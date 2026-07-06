package com.brickdeck.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "brickdeck.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}

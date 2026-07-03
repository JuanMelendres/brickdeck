package com.brickdeck.api.external.rebrickable.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rebrickable")
public record RebrickableProperties(
        String baseUrl,
        String apiKey
) {
}
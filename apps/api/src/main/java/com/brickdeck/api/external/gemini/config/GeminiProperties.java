package com.brickdeck.api.external.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String baseUrl,
        String apiKey,
        String model
) {
}

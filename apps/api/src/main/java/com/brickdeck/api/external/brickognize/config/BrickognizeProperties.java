package com.brickdeck.api.external.brickognize.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brickognize")
public record BrickognizeProperties(
        String baseUrl
) {
}

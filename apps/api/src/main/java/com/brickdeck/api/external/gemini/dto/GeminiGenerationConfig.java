package com.brickdeck.api.external.gemini.dto;

import java.util.Map;

public record GeminiGenerationConfig(
        String responseMimeType,
        Map<String, Object> responseSchema
) {
}

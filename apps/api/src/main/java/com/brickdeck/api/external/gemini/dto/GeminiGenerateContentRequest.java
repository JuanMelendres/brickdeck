package com.brickdeck.api.external.gemini.dto;

import java.util.List;

public record GeminiGenerateContentRequest(
        List<GeminiContent> contents,
        GeminiGenerationConfig generationConfig
) {
}

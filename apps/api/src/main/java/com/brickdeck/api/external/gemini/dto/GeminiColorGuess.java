package com.brickdeck.api.external.gemini.dto;

public record GeminiColorGuess(
        String colorName,
        Double confidence,
        String reasoning
) {
}

package com.brickdeck.api.classification.dto;

public record ColorSuggestion(
        Integer colorId,
        String colorName,
        Double confidence
) {
}

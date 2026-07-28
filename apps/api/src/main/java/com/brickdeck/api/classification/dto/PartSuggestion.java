package com.brickdeck.api.classification.dto;

public record PartSuggestion(
        String partNumber,
        String partNameGuess,
        Double score,
        PartResolutionStatus resolutionStatus,
        String referenceImageUrl
) {
}

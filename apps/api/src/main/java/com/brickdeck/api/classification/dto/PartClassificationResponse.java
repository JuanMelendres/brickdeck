package com.brickdeck.api.classification.dto;

import java.util.List;

public record PartClassificationResponse(
        ColorSuggestion colorSuggestion,
        List<PartSuggestion> partSuggestions
) {
}

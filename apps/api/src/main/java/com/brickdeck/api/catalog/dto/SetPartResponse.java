package com.brickdeck.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SetPartResponse(
        UUID id,
        String setNumber,
        String partNumber,
        String partName,
        @Schema(nullable = true) String partImageUrl,
        @Schema(nullable = true) Integer colorExternalId,
        String colorName,
        @Schema(nullable = true) String colorRgb,
        Integer quantity,
        boolean spare,
        @Schema(nullable = true) String elementId
) {
}

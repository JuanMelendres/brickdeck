package com.brickdeck.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SetPartResponse(
        UUID id,
        String setNumber,
        String partNumber,
        String partName,
        // Not @Schema(nullable = true): same no-effect quirk as
        // BrickSetResponse.id - CI showed it doesn't apply here either.
        String partImageUrl,
        @Schema(nullable = true) Integer colorExternalId,
        String colorName,
        @Schema(nullable = true) String colorRgb,
        Integer quantity,
        boolean spare,
        @Schema(nullable = true) String elementId
) {
}

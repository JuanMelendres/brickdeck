package com.brickdeck.api.catalog.dto;

import java.util.UUID;

public record SetPartResponse(
        UUID id,
        String setNumber,
        String partNumber,
        String partName,
        String partImageUrl,
        Integer colorExternalId,
        String colorName,
        String colorRgb,
        Integer quantity,
        boolean spare,
        String elementId
) {
}

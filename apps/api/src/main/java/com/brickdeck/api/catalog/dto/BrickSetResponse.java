package com.brickdeck.api.catalog.dto;

import java.util.UUID;

public record BrickSetResponse(
        UUID id,
        String externalSetNumber,
        String name,
        Integer yearReleased,
        UUID themeId,
        String themeName,
        Integer numberOfParts,
        String imageUrl,
        String source
) {
}
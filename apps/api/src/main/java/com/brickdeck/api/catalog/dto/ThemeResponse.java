package com.brickdeck.api.catalog.dto;

import java.util.UUID;

public record ThemeResponse(
        UUID id,
        String externalId,
        String name,
        UUID parentThemeId
) {
}
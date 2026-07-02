package com.brickdeck.api.health;

import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String service,
        String version,
        OffsetDateTime timestamp
) {
}
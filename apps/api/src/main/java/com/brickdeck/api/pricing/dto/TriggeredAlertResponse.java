package com.brickdeck.api.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A recorded firing of a price-alert rule. */
public record TriggeredAlertResponse(
        UUID id,
        UUID ruleId,
        String setNumber,
        BigDecimal amount,
        String currency,
        String message,
        LocalDateTime triggeredAt
) {
}

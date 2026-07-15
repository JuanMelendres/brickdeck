package com.brickdeck.api.pricing.dto;

import com.brickdeck.api.pricing.entity.PriceAlertType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A stored price-alert rule. */
public record PriceAlertRuleResponse(
        UUID id,
        String setNumber,
        String currency,
        PriceAlertType type,
        BigDecimal thresholdValue,
        boolean active,
        LocalDateTime createdAt
) {
}

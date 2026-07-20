package com.brickdeck.api.pricing.dto;

import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.entity.PriceSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A stored price snapshot. */
public record PriceSnapshotResponse(
        UUID id,
        String setNumber,
        BigDecimal amount,
        String currency,
        PriceCondition condition,
        PriceSource source,
        LocalDate observedAt,
        String store,
        String url,
        LocalDateTime createdAt
) {
}

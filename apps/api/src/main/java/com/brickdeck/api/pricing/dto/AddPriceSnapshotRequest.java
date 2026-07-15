package com.brickdeck.api.pricing.dto;

import com.brickdeck.api.pricing.entity.PriceCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to record an observed price for a set. The set is resolved from the
 * local catalog (find-or-import); an unresolvable set returns 404. {@code store}
 * and {@code url} are optional.
 */
public record AddPriceSnapshotRequest(
        @NotBlank String setNumber,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code") String currency,
        @NotNull PriceCondition condition,
        @NotNull @PastOrPresent LocalDate observedAt,
        @Size(max = 255) String store,
        @Size(max = 1024) String url
) {
}

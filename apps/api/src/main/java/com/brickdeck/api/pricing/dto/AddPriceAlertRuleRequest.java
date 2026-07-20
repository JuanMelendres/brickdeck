package com.brickdeck.api.pricing.dto;

import com.brickdeck.api.pricing.entity.PriceAlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Request to create a price-alert rule for a set already in the owner's
 * wishlist. {@code thresholdValue} is required for target/percent types and
 * validated in the service (not here, since its rules depend on {@code type}).
 */
public record AddPriceAlertRuleRequest(
        @NotBlank String setNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code") String currency,
        @NotNull PriceAlertType type,
        BigDecimal thresholdValue
) {
}

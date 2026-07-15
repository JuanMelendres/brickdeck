package com.brickdeck.api.pricing.service;

import com.brickdeck.api.pricing.entity.PriceAlertType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PriceAlertRuleEvaluator {
    public Optional<String> evaluate(PriceAlertType type, BigDecimal thresholdValue,
                                     BigDecimal amount, BigDecimal average, BigDecimal lowest) {
        return switch (type) {
            case BELOW_TARGET_PRICE -> amount.compareTo(thresholdValue) < 0
                    ? Optional.of(amount + " is below your target " + thresholdValue) : Optional.empty();
            case PERCENT_BELOW_AVERAGE -> {
                BigDecimal cutoff = average.multiply(
                        BigDecimal.ONE.subtract(thresholdValue.movePointLeft(2)));
                yield amount.compareTo(cutoff) <= 0
                        ? Optional.of(amount + " is at least " + thresholdValue + "% below average " + average)
                        : Optional.empty();
            }
            case AT_OR_BELOW_LOWEST -> amount.compareTo(lowest) <= 0
                    ? Optional.of(amount + " is at or below your lowest " + lowest) : Optional.empty();
        };
    }
}

package com.brickdeck.api.pricing.service;

import com.brickdeck.api.pricing.entity.PriceAlertType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class PriceAlertRuleEvaluatorTest {
    private final PriceAlertRuleEvaluator evaluator = new PriceAlertRuleEvaluator();

    @Test
    void belowTargetFiresWhenStrictlyUnder() {
        assertThat(evaluator.evaluate(PriceAlertType.BELOW_TARGET_PRICE,
                new BigDecimal("100"), new BigDecimal("99.99"), null, null)).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.BELOW_TARGET_PRICE,
                new BigDecimal("100"), new BigDecimal("100"), null, null)).isEmpty();
    }

    @Test
    void percentBelowAverageFiresAtOrUnderThreshold() {
        // avg 100, 20% => fires at <= 80
        assertThat(evaluator.evaluate(PriceAlertType.PERCENT_BELOW_AVERAGE,
                new BigDecimal("20"), new BigDecimal("80"), new BigDecimal("100"), null)).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.PERCENT_BELOW_AVERAGE,
                new BigDecimal("20"), new BigDecimal("80.01"), new BigDecimal("100"), null)).isEmpty();
    }

    @Test
    void atOrBelowLowestFiresWhenTyingOrBeating() {
        assertThat(evaluator.evaluate(PriceAlertType.AT_OR_BELOW_LOWEST,
                null, new BigDecimal("80"), null, new BigDecimal("80"))).isPresent();
        assertThat(evaluator.evaluate(PriceAlertType.AT_OR_BELOW_LOWEST,
                null, new BigDecimal("80.01"), null, new BigDecimal("80"))).isEmpty();
    }
}

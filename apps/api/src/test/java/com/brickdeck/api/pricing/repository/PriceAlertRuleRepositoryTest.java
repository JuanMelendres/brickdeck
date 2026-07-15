package com.brickdeck.api.pricing.repository;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.pricing.entity.PriceAlertRule;
import com.brickdeck.api.pricing.entity.PriceAlertType;
import com.brickdeck.api.security.entity.User;
import com.brickdeck.api.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PriceAlertRuleRepositoryTest {

    @Autowired
    private PriceAlertRuleRepository priceAlertRuleRepository;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private UserRepository userRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void findsRuleByUserSetAndCurrencyAndScopesByOwner() {
        String setNumber = "IT-ALERT-" + suffix;
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(setNumber);
        set.setName("Alerted Set");
        set.setNumberOfParts(100);
        set.setSource("TEST");
        brickSetRepository.saveAndFlush(set);

        User owner = user("alert-" + suffix + "@brickdeck.test");
        User other = user("alert-other-" + suffix + "@brickdeck.test");

        PriceAlertRule rule = new PriceAlertRule();
        rule.setUser(owner);
        rule.setBrickSet(set);
        rule.setCurrency("USD");
        rule.setType(PriceAlertType.BELOW_TARGET_PRICE);
        rule.setThresholdValue(new BigDecimal("100.00"));
        rule.setActive(true);
        PriceAlertRule saved = priceAlertRuleRepository.saveAndFlush(rule);

        List<PriceAlertRule> found = priceAlertRuleRepository
                .findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(
                        owner.getId(), setNumber, "USD");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(saved.getId());

        Optional<PriceAlertRule> notOwned = priceAlertRuleRepository
                .findByIdAndUserId(saved.getId(), other.getId());
        assertThat(notOwned).isEmpty();
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setRole("USER");
        return userRepository.saveAndFlush(user);
    }
}

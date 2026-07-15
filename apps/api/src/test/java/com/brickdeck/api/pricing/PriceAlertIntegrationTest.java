package com.brickdeck.api.pricing;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.pricing.dto.AddPriceAlertRuleRequest;
import com.brickdeck.api.pricing.dto.AddPriceSnapshotRequest;
import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.entity.PriceAlertType;
import com.brickdeck.api.pricing.service.PriceAlertService;
import com.brickdeck.api.pricing.service.PriceSnapshotService;
import com.brickdeck.api.security.entity.User;
import com.brickdeck.api.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PriceAlertIntegrationTest {

    @Autowired
    private PriceAlertService priceAlertService;
    @Autowired
    private PriceSnapshotService priceSnapshotService;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private UserSetRepository userSetRepository;
    @Autowired
    private UserRepository userRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void firesAnAlertWhenASnapshotMeetsARuleAgainstRealDatabase() {
        String setNumber = "IT-ALERT-" + suffix;
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(setNumber);
        set.setName("Alerted Set");
        set.setNumberOfParts(100);
        set.setSource("TEST");
        brickSetRepository.saveAndFlush(set);

        User owner = user("alert-" + suffix + "@brickdeck.test");
        User other = user("alert-other-" + suffix + "@brickdeck.test");
        wishlist(owner, set);

        // Rule: alert when a USD price drops below 100.
        priceAlertService.createRule(owner, new AddPriceAlertRuleRequest(
                setNumber, "USD", PriceAlertType.BELOW_TARGET_PRICE, new BigDecimal("100")));

        // A snapshot at 80 fires the rule.
        addSnapshot(owner, setNumber, "80");

        assertThat(priceAlertService.listTriggered(owner, PageRequest.of(0, 20)).totalElements())
                .isEqualTo(1);
        assertThat(priceAlertService.listTriggered(owner, PageRequest.of(0, 20))
                .content().get(0).amount()).isEqualByComparingTo("80");

        // A snapshot at 150 does not fire (still one triggered alert).
        addSnapshot(owner, setNumber, "150");
        assertThat(priceAlertService.listTriggered(owner, PageRequest.of(0, 20)).totalElements())
                .isEqualTo(1);

        // Another user (no rules) sees nothing.
        assertThat(priceAlertService.listTriggered(other, PageRequest.of(0, 20)).totalElements())
                .isZero();
    }

    private void addSnapshot(User owner, String setNumber, String amount) {
        priceSnapshotService.addSnapshot(owner, new AddPriceSnapshotRequest(
                setNumber, new BigDecimal(amount), "USD",
                PriceCondition.NEW, LocalDate.of(2026, 1, 10), null, null));
    }

    private void wishlist(User owner, BrickSet set) {
        UserSet us = new UserSet();
        us.setUser(owner);
        us.setBrickSet(set);
        us.setStatus(CollectionStatus.WISHLIST);
        userSetRepository.saveAndFlush(us);
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setRole("USER");
        return userRepository.saveAndFlush(user);
    }
}

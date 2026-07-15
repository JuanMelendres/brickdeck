package com.brickdeck.api.pricing;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.AddPriceSnapshotRequest;
import com.brickdeck.api.pricing.dto.DealVerdict;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.dto.PriceSnapshotResponse;
import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.service.PriceAnalysisService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PriceTrackingIntegrationTest {

    @Autowired
    private PriceSnapshotService priceSnapshotService;
    @Autowired
    private PriceAnalysisService priceAnalysisService;
    @Autowired
    private BrickSetRepository brickSetRepository;
    @Autowired
    private UserRepository userRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void recordsSnapshotsAndAnalyzesPerCurrencyAgainstRealDatabase() {
        String setNumber = "IT-PRICE-" + suffix;
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(setNumber);
        set.setName("Priced Set");
        set.setNumberOfParts(100);
        set.setSource("TEST");
        brickSetRepository.saveAndFlush(set);

        User owner = user("price-" + suffix + "@brickdeck.test");
        User other = user("price-other-" + suffix + "@brickdeck.test");

        snapshot(owner, setNumber, "80", "USD");
        snapshot(owner, setNumber, "100", "USD");
        snapshot(owner, setNumber, "120", "USD");
        snapshot(owner, setNumber, "2000", "MXN"); // must not leak into USD analysis

        PriceAnalysisResponse usd = priceAnalysisService.analyze(
                owner.getId(), setNumber, "USD", new BigDecimal("80"));

        assertThat(usd.snapshotCount()).isEqualTo(3);
        assertThat(usd.minAmount()).isEqualByComparingTo("80");
        assertThat(usd.maxAmount()).isEqualByComparingTo("120");
        assertThat(usd.averageAmount()).isEqualByComparingTo("100.00");
        assertThat(usd.pricePerPiece()).isEqualByComparingTo("1.00");
        assertThat(usd.candidate().verdict()).isEqualTo(DealVerdict.GREAT_DEAL);
        assertThat(usd.candidate().atOrBelowLowest()).isTrue();

        // Owner-scoped list filtered by set number.
        PageResponse<PriceSnapshotResponse> page = priceSnapshotService.findForUser(
                owner, setNumber, PageRequest.of(0, 20));
        assertThat(page.totalElements()).isEqualTo(4);

        // Another user has no snapshots -> 404.
        assertThatThrownBy(() -> priceAnalysisService.analyze(
                other.getId(), setNumber, "USD", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void snapshot(User owner, String setNumber, String amount, String currency) {
        priceSnapshotService.addSnapshot(owner, new AddPriceSnapshotRequest(
                setNumber, new BigDecimal(amount), currency,
                PriceCondition.NEW, LocalDate.of(2026, 1, 10), null, null));
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setRole("USER");
        return userRepository.saveAndFlush(user);
    }
}

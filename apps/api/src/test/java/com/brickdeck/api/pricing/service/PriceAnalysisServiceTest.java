package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.CandidateEvaluation;
import com.brickdeck.api.pricing.dto.DealVerdict;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.entity.PriceSource;
import com.brickdeck.api.pricing.repository.PriceSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAnalysisServiceTest {

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;

    @InjectMocks
    private PriceAnalysisService service;

    private final UUID userId = UUID.randomUUID();

    private BrickSet set(Integer numberOfParts) {
        BrickSet s = new BrickSet();
        s.setExternalSetNumber("100-1");
        s.setName("Test Set");
        s.setNumberOfParts(numberOfParts);
        return s;
    }

    private PriceSnapshot snapshot(BrickSet set, String amount, LocalDate observedAt) {
        PriceSnapshot p = new PriceSnapshot();
        p.setId(UUID.randomUUID());
        p.setBrickSet(set);
        p.setSource(PriceSource.MANUAL);
        p.setCondition(PriceCondition.NEW);
        p.setCurrency("USD");
        p.setAmount(new BigDecimal(amount));
        p.setObservedAt(observedAt);
        return p;
    }

    private void haveSnapshots(BrickSet set) {
        when(priceSnapshotRepository
                .findByUserIdAndBrickSet_ExternalSetNumberAndCurrency(userId, "100-1", "USD"))
                .thenReturn(List.of(
                        snapshot(set, "100", LocalDate.of(2026, 1, 2)),
                        snapshot(set, "120", LocalDate.of(2026, 1, 3)), // latest
                        snapshot(set, "80", LocalDate.of(2026, 1, 1))));
    }

    @Test
    void aggregatesMinAvgMaxLatestAndPricePerPiece() {
        haveSnapshots(set(100));

        PriceAnalysisResponse r = service.analyze(userId, "100-1", "USD", null);

        assertThat(r.snapshotCount()).isEqualTo(3);
        assertThat(r.minAmount()).isEqualByComparingTo("80");
        assertThat(r.maxAmount()).isEqualByComparingTo("120");
        assertThat(r.averageAmount()).isEqualByComparingTo("100.00");
        assertThat(r.latestAmount()).isEqualByComparingTo("120");
        assertThat(r.numberOfParts()).isEqualTo(100);
        assertThat(r.pricePerPiece()).isEqualByComparingTo("1.00");
        assertThat(r.candidate()).isNull();
    }

    @Test
    void ratesAPriceAtOrBelowTheLowestAsGreatDeal() {
        haveSnapshots(set(100));

        CandidateEvaluation c = service.analyze(userId, "100-1", "USD",
                new BigDecimal("80")).candidate();

        assertThat(c.verdict()).isEqualTo(DealVerdict.GREAT_DEAL);
        assertThat(c.atOrBelowLowest()).isTrue();
        assertThat(c.percentBelowAverage()).isEqualByComparingTo("20.0");
        assertThat(c.pricePerPiece()).isEqualByComparingTo("0.80");
    }

    @Test
    void ratesAPriceTenPercentBelowAverageAsGoodDeal() {
        haveSnapshots(set(100));

        CandidateEvaluation c = service.analyze(userId, "100-1", "USD",
                new BigDecimal("85")).candidate();

        assertThat(c.verdict()).isEqualTo(DealVerdict.GOOD_DEAL);
        assertThat(c.atOrBelowLowest()).isFalse();
    }

    @Test
    void ratesAPriceJustUnderAverageAsFair() {
        haveSnapshots(set(100));

        CandidateEvaluation c = service.analyze(userId, "100-1", "USD",
                new BigDecimal("95")).candidate();

        assertThat(c.verdict()).isEqualTo(DealVerdict.FAIR);
    }

    @Test
    void ratesAPriceAboveAverageAsPoor() {
        haveSnapshots(set(100));

        CandidateEvaluation c = service.analyze(userId, "100-1", "USD",
                new BigDecimal("110")).candidate();

        assertThat(c.verdict()).isEqualTo(DealVerdict.POOR);
        assertThat(c.percentBelowAverage()).isEqualByComparingTo("-10.0");
    }

    @Test
    void omitsPricePerPieceWhenTheSetHasNoPartCount() {
        haveSnapshots(set(null));

        PriceAnalysisResponse r = service.analyze(userId, "100-1", "USD",
                new BigDecimal("80"));

        assertThat(r.numberOfParts()).isNull();
        assertThat(r.pricePerPiece()).isNull();
        assertThat(r.candidate().pricePerPiece()).isNull();
    }

    @Test
    void throwsWhenNoSnapshotsForTheSetAndCurrency() {
        when(priceSnapshotRepository
                .findByUserIdAndBrickSet_ExternalSetNumberAndCurrency(userId, "100-1", "USD"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.analyze(userId, "100-1", "USD", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No price snapshots");
    }
}

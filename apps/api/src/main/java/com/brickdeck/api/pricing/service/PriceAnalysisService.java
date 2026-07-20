package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.CandidateEvaluation;
import com.brickdeck.api.pricing.dto.DealVerdict;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.repository.PriceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Computes a per-set, per-currency price analysis from the user's snapshots and,
 * when a candidate price is given, a deal verdict. Pure given the loaded
 * snapshots; no external calls.
 */
@Service
public class PriceAnalysisService {

    /** A candidate at or below 90% of the average is a good deal. */
    private static final BigDecimal GOOD_DEAL_FACTOR = new BigDecimal("0.90");

    private final PriceSnapshotRepository priceSnapshotRepository;

    public PriceAnalysisService(PriceSnapshotRepository priceSnapshotRepository) {
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public PriceAnalysisResponse analyze(
            UUID userId, String setNumber, String currency, BigDecimal candidatePrice) {

        String normalized = SetNumbers.normalize(setNumber);
        List<PriceSnapshot> snapshots = priceSnapshotRepository
                .findByUserIdAndBrickSet_ExternalSetNumberAndCurrency(userId, normalized, currency);

        if (snapshots.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No price snapshots for set " + normalized + " in " + currency);
        }

        BigDecimal min = snapshots.stream().map(PriceSnapshot::getAmount)
                .min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = snapshots.stream().map(PriceSnapshot::getAmount)
                .max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal sum = snapshots.stream().map(PriceSnapshot::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(
                BigDecimal.valueOf(snapshots.size()), 2, RoundingMode.HALF_UP);
        BigDecimal latest = snapshots.stream()
                .max(Comparator.comparing(PriceSnapshot::getObservedAt))
                .map(PriceSnapshot::getAmount).orElseThrow();

        Integer numberOfParts = snapshots.get(0).getBrickSet().getNumberOfParts();
        BigDecimal averagePpp = pricePerPiece(average, numberOfParts);

        CandidateEvaluation candidate = candidatePrice == null
                ? null
                : evaluate(candidatePrice, min, average, numberOfParts);

        return new PriceAnalysisResponse(
                normalized, currency, snapshots.size(),
                min, average, max, latest, numberOfParts, averagePpp, candidate);
    }

    private CandidateEvaluation evaluate(
            BigDecimal candidate, BigDecimal min, BigDecimal average, Integer numberOfParts) {

        boolean atOrBelowLowest = candidate.compareTo(min) <= 0;
        BigDecimal percentBelowAverage = average.subtract(candidate)
                .multiply(BigDecimal.valueOf(100))
                .divide(average, 1, RoundingMode.HALF_UP);

        DealVerdict verdict;
        if (atOrBelowLowest) {
            verdict = DealVerdict.GREAT_DEAL;
        } else if (candidate.compareTo(average.multiply(GOOD_DEAL_FACTOR)) <= 0) {
            verdict = DealVerdict.GOOD_DEAL;
        } else if (candidate.compareTo(average) <= 0) {
            verdict = DealVerdict.FAIR;
        } else {
            verdict = DealVerdict.POOR;
        }

        return new CandidateEvaluation(
                candidate, pricePerPiece(candidate, numberOfParts),
                percentBelowAverage, atOrBelowLowest, verdict);
    }

    private BigDecimal pricePerPiece(BigDecimal amount, Integer numberOfParts) {
        if (numberOfParts == null || numberOfParts <= 0) {
            return null;
        }
        return amount.divide(BigDecimal.valueOf(numberOfParts), 4, RoundingMode.HALF_UP);
    }
}

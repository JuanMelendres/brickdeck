package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.catalog.service.SetNumbers;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.AddPriceAlertRuleRequest;
import com.brickdeck.api.pricing.dto.PriceAlertRuleResponse;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.dto.TriggeredAlertResponse;
import com.brickdeck.api.pricing.entity.PriceAlertRule;
import com.brickdeck.api.pricing.entity.PriceAlertType;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.entity.TriggeredAlert;
import com.brickdeck.api.pricing.repository.PriceAlertRuleRepository;
import com.brickdeck.api.pricing.repository.TriggeredAlertRepository;
import com.brickdeck.api.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PriceAlertService {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");

    private final PriceAlertRuleRepository ruleRepository;
    private final TriggeredAlertRepository triggeredAlertRepository;
    private final UserSetRepository userSetRepository;
    private final PriceAnalysisService priceAnalysisService;
    private final PriceAlertRuleEvaluator evaluator;
    private final BrickSetService brickSetService;

    public PriceAlertService(
            PriceAlertRuleRepository ruleRepository,
            TriggeredAlertRepository triggeredAlertRepository,
            UserSetRepository userSetRepository,
            PriceAnalysisService priceAnalysisService,
            PriceAlertRuleEvaluator evaluator,
            BrickSetService brickSetService) {
        this.ruleRepository = ruleRepository;
        this.triggeredAlertRepository = triggeredAlertRepository;
        this.userSetRepository = userSetRepository;
        this.priceAnalysisService = priceAnalysisService;
        this.evaluator = evaluator;
        this.brickSetService = brickSetService;
    }

    @Transactional
    public PriceAlertRuleResponse createRule(User owner, AddPriceAlertRuleRequest request) {
        String normalized = SetNumbers.normalize(request.setNumber());

        boolean inWishlist = userSetRepository
                .findByUserIdAndStatus(owner.getId(), CollectionStatus.WISHLIST).stream()
                .anyMatch(us -> us.getBrickSet().getExternalSetNumber().equals(normalized));
        if (!inWishlist) {
            throw new ResourceNotFoundException(
                    "Set not in your wishlist: " + normalized
                            + " (add it to your wishlist to set a price alert)");
        }

        validateThreshold(request.type(), request.thresholdValue());

        BrickSet set = brickSetService.findOrImportEntity(normalized);

        PriceAlertRule rule = new PriceAlertRule();
        rule.setUser(owner);
        rule.setBrickSet(set);
        rule.setCurrency(request.currency());
        rule.setType(request.type());
        rule.setThresholdValue(
                request.type() == PriceAlertType.AT_OR_BELOW_LOWEST ? null : request.thresholdValue());
        rule.setActive(true);

        return toRuleResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public PageResponse<PriceAlertRuleResponse> listRules(User owner, Pageable pageable) {
        Page<PriceAlertRule> page = ruleRepository.findByUserId(owner.getId(), pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toRuleResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void deleteRule(User owner, UUID id) {
        PriceAlertRule rule = ruleRepository.findByIdAndUserId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Price alert rule not found: " + id));
        ruleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public PageResponse<TriggeredAlertResponse> listTriggered(User owner, Pageable pageable) {
        Page<TriggeredAlert> page = triggeredAlertRepository.findByUserId(owner.getId(), pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toTriggeredResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void deleteTriggered(User owner, UUID id) {
        TriggeredAlert alert = triggeredAlertRepository.findByIdAndUserId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Triggered alert not found: " + id));
        triggeredAlertRepository.delete(alert);
    }

    /**
     * Evaluate the user's active rules for the snapshot's set and currency,
     * persisting a triggered alert per matching rule. Called after a snapshot is
     * saved, so at least one snapshot exists for the analysis.
     */
    @Transactional
    public void evaluateForSnapshot(User owner, PriceSnapshot snapshot) {
        String setNumber = snapshot.getBrickSet().getExternalSetNumber();
        String currency = snapshot.getCurrency();

        List<PriceAlertRule> rules = ruleRepository
                .findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(
                        owner.getId(), setNumber, currency);
        if (rules.isEmpty()) {
            return;
        }

        PriceAnalysisResponse analysis =
                priceAnalysisService.analyze(owner.getId(), setNumber, currency, null);

        for (PriceAlertRule rule : rules) {
            evaluator.evaluate(rule.getType(), rule.getThresholdValue(),
                            snapshot.getAmount(), analysis.averageAmount(), analysis.minAmount())
                    .ifPresent(message -> saveTriggered(owner, rule, snapshot, message));
        }
    }

    private void saveTriggered(User owner, PriceAlertRule rule, PriceSnapshot snapshot, String message) {
        TriggeredAlert alert = new TriggeredAlert();
        alert.setUser(owner);
        alert.setRule(rule);
        alert.setSnapshot(snapshot);
        alert.setAmount(snapshot.getAmount());
        alert.setCurrency(snapshot.getCurrency());
        alert.setMessage(message);
        triggeredAlertRepository.save(alert);
    }

    private void validateThreshold(PriceAlertType type, BigDecimal threshold) {
        switch (type) {
            case BELOW_TARGET_PRICE -> require(threshold != null && threshold.signum() > 0,
                    "thresholdValue must be a positive amount for BELOW_TARGET_PRICE");
            case PERCENT_BELOW_AVERAGE -> require(threshold != null && threshold.signum() > 0
                            && threshold.compareTo(MAX_PERCENT) <= 0,
                    "thresholdValue must be a percent in the range (0, 100] for PERCENT_BELOW_AVERAGE");
            case AT_OR_BELOW_LOWEST -> {
                // threshold ignored
            }
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private PriceAlertRuleResponse toRuleResponse(PriceAlertRule rule) {
        return new PriceAlertRuleResponse(
                rule.getId(),
                rule.getBrickSet().getExternalSetNumber(),
                rule.getCurrency(),
                rule.getType(),
                rule.getThresholdValue(),
                rule.isActive(),
                rule.getCreatedAt());
    }

    private TriggeredAlertResponse toTriggeredResponse(TriggeredAlert alert) {
        return new TriggeredAlertResponse(
                alert.getId(),
                alert.getRule().getId(),
                alert.getSnapshot().getBrickSet().getExternalSetNumber(),
                alert.getAmount(),
                alert.getCurrency(),
                alert.getMessage(),
                alert.getTriggeredAt());
    }
}

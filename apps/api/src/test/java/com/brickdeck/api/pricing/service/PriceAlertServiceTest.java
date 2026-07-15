package com.brickdeck.api.pricing.service;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import com.brickdeck.api.collection.repository.UserSetRepository;
import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.AddPriceAlertRuleRequest;
import com.brickdeck.api.pricing.dto.PriceAlertRuleResponse;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.entity.PriceAlertRule;
import com.brickdeck.api.pricing.entity.PriceAlertType;
import com.brickdeck.api.pricing.entity.PriceSnapshot;
import com.brickdeck.api.pricing.repository.PriceAlertRuleRepository;
import com.brickdeck.api.pricing.repository.TriggeredAlertRepository;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock
    private PriceAlertRuleRepository ruleRepository;
    @Mock
    private TriggeredAlertRepository triggeredAlertRepository;
    @Mock
    private UserSetRepository userSetRepository;
    @Mock
    private PriceAnalysisService priceAnalysisService;
    @Mock
    private PriceAlertRuleEvaluator evaluator;
    @Mock
    private BrickSetService brickSetService;

    @InjectMocks
    private PriceAlertService service;

    private User owner;
    private BrickSet set;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        set = new BrickSet();
        set.setExternalSetNumber("75257-1");
        set.setName("Falcon");
    }

    private void wishlistContains(BrickSet s) {
        UserSet us = new UserSet();
        us.setBrickSet(s);
        us.setStatus(CollectionStatus.WISHLIST);
        when(userSetRepository.findByUserIdAndStatus(owner.getId(), CollectionStatus.WISHLIST))
                .thenReturn(List.of(us));
    }

    private AddPriceAlertRuleRequest request(PriceAlertType type, BigDecimal threshold) {
        return new AddPriceAlertRuleRequest("75257-1", "USD", type, threshold);
    }

    @Test
    void createRuleRejectsASetNotInWishlist() {
        when(userSetRepository.findByUserIdAndStatus(owner.getId(), CollectionStatus.WISHLIST))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.createRule(owner,
                request(PriceAlertType.BELOW_TARGET_PRICE, new BigDecimal("100"))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRuleRejectsMissingThresholdForTargetPrice() {
        wishlistContains(set);

        assertThatThrownBy(() -> service.createRule(owner,
                request(PriceAlertType.BELOW_TARGET_PRICE, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRuleRejectsPercentAboveOneHundred() {
        wishlistContains(set);

        assertThatThrownBy(() -> service.createRule(owner,
                request(PriceAlertType.PERCENT_BELOW_AVERAGE, new BigDecimal("150"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRulePersistsAtOrBelowLowestWithNullThreshold() {
        wishlistContains(set);
        when(brickSetService.findOrImportEntity("75257-1")).thenReturn(set);
        when(ruleRepository.save(any(PriceAlertRule.class))).thenAnswer(i -> i.getArgument(0));

        PriceAlertRuleResponse response = service.createRule(owner,
                request(PriceAlertType.AT_OR_BELOW_LOWEST, null));

        assertThat(response.type()).isEqualTo(PriceAlertType.AT_OR_BELOW_LOWEST);
        assertThat(response.thresholdValue()).isNull();
        assertThat(response.setNumber()).isEqualTo("75257-1");
    }

    @Test
    void evaluateForSnapshotSavesAnAlertWhenARuleFires() {
        PriceAlertRule rule = new PriceAlertRule();
        rule.setUser(owner);
        rule.setBrickSet(set);
        rule.setCurrency("USD");
        rule.setType(PriceAlertType.BELOW_TARGET_PRICE);
        rule.setThresholdValue(new BigDecimal("100"));
        rule.setActive(true);

        PriceSnapshot snapshot = snapshot(new BigDecimal("80"));

        when(ruleRepository.findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(
                owner.getId(), "75257-1", "USD")).thenReturn(List.of(rule));
        when(priceAnalysisService.analyze(owner.getId(), "75257-1", "USD", null))
                .thenReturn(new PriceAnalysisResponse(
                        "75257-1", "USD", 1, new BigDecimal("80"), new BigDecimal("100"),
                        new BigDecimal("120"), new BigDecimal("80"), 100, new BigDecimal("1.00"), null));
        when(evaluator.evaluate(eq(PriceAlertType.BELOW_TARGET_PRICE), eq(new BigDecimal("100")),
                eq(new BigDecimal("80")), eq(new BigDecimal("100")), eq(new BigDecimal("80"))))
                .thenReturn(Optional.of("fired"));

        service.evaluateForSnapshot(owner, snapshot);

        verify(triggeredAlertRepository).save(any());
    }

    @Test
    void evaluateForSnapshotDoesNothingWhenNoRules() {
        PriceSnapshot snapshot = snapshot(new BigDecimal("80"));
        when(ruleRepository.findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(
                owner.getId(), "75257-1", "USD")).thenReturn(List.of());

        service.evaluateForSnapshot(owner, snapshot);

        verify(priceAnalysisService, never()).analyze(any(), any(), any(), any());
        verify(triggeredAlertRepository, never()).save(any());
    }

    @Test
    void deleteRuleThrowsWhenNotOwned() {
        UUID id = UUID.randomUUID();
        when(ruleRepository.findByIdAndUserId(id, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRule(owner, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PriceSnapshot snapshot(BigDecimal amount) {
        PriceSnapshot s = new PriceSnapshot();
        s.setId(UUID.randomUUID());
        s.setUser(owner);
        s.setBrickSet(set);
        s.setCurrency("USD");
        s.setAmount(amount);
        s.setObservedAt(LocalDate.of(2026, 1, 10));
        return s;
    }
}

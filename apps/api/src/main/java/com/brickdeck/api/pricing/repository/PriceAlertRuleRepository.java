package com.brickdeck.api.pricing.repository;

import com.brickdeck.api.pricing.entity.PriceAlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceAlertRuleRepository extends JpaRepository<PriceAlertRule, UUID> {

    @EntityGraph(attributePaths = {"brickSet"})
    Page<PriceAlertRule> findByUserId(UUID userId, Pageable pageable);

    Optional<PriceAlertRule> findByIdAndUserId(UUID id, UUID userId);

    List<PriceAlertRule> findByUserIdAndBrickSet_ExternalSetNumberAndCurrencyAndActiveTrue(
            UUID userId, String externalSetNumber, String currency);
}

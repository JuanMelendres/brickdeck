package com.brickdeck.api.pricing.repository;

import com.brickdeck.api.pricing.entity.PriceSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, UUID> {

    @EntityGraph(attributePaths = {"brickSet"})
    Page<PriceSnapshot> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"brickSet"})
    Page<PriceSnapshot> findByUserIdAndBrickSet_ExternalSetNumber(
            UUID userId, String externalSetNumber, Pageable pageable);

    @EntityGraph(attributePaths = {"brickSet"})
    List<PriceSnapshot> findByUserIdAndBrickSet_ExternalSetNumberAndCurrency(
            UUID userId, String externalSetNumber, String currency);

    @EntityGraph(attributePaths = {"brickSet"})
    Optional<PriceSnapshot> findByIdAndUserId(UUID id, UUID userId);
}

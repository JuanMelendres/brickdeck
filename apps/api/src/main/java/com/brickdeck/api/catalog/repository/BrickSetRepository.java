package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.BrickSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrickSetRepository extends JpaRepository<BrickSet, UUID> {

    Optional<BrickSet> findByExternalSetNumber(String externalSetNumber);
}
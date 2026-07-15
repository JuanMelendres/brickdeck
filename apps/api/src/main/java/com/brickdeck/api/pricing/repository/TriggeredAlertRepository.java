package com.brickdeck.api.pricing.repository;

import com.brickdeck.api.pricing.entity.TriggeredAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TriggeredAlertRepository extends JpaRepository<TriggeredAlert, UUID> {

    Page<TriggeredAlert> findByUserId(UUID userId, Pageable pageable);

    Optional<TriggeredAlert> findByIdAndUserId(UUID id, UUID userId);
}

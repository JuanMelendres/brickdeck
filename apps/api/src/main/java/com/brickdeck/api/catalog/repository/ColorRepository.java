package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ColorRepository extends JpaRepository<Color, UUID> {

    Optional<Color> findByExternalId(Integer externalId);
}

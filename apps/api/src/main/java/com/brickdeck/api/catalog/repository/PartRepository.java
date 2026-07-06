package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {

    Optional<Part> findByExternalPartNumber(String externalPartNumber);
}

package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.entity.SetPart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SetPartRepository extends JpaRepository<SetPart, UUID> {

    Page<SetPart> findByBrickSet_ExternalSetNumber(String externalSetNumber, Pageable pageable);

    Optional<SetPart> findByBrickSetAndPartAndColorAndSpare(
            BrickSet brickSet, Part part, Color color, boolean spare);

    /** Required (non-spare) inventory lines of a set, with part and color loaded. */
    @EntityGraph(attributePaths = {"part", "color"})
    List<SetPart> findByBrickSet_ExternalSetNumberAndSpareFalse(String externalSetNumber);
}

package com.brickdeck.api.collection.repository;

import com.brickdeck.api.collection.entity.UserSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSetRepository extends JpaRepository<UserSet, UUID> {

    boolean existsByUserIdAndBrickSetId(UUID userId, UUID brickSetId);

    @EntityGraph(attributePaths = {"brickSet", "brickSet.theme"})
    Page<UserSet> findByUserId(UUID userId, Pageable pageable);
}

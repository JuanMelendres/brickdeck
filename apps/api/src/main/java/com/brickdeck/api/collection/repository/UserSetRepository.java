package com.brickdeck.api.collection.repository;

import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.entity.UserSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSetRepository extends JpaRepository<UserSet, UUID> {

    boolean existsByUserIdAndBrickSetId(UUID userId, UUID brickSetId);

    @EntityGraph(attributePaths = {"brickSet", "brickSet.theme"})
    Page<UserSet> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"brickSet", "brickSet.theme"})
    List<UserSet> findByUserIdAndStatus(UUID userId, CollectionStatus status);

    @EntityGraph(attributePaths = {"brickSet", "brickSet.theme"})
    Optional<UserSet> findByIdAndUserId(UUID id, UUID userId);
}

package com.brickdeck.api.collection.repository;

import com.brickdeck.api.collection.entity.UserPart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPartRepository extends JpaRepository<UserPart, UUID> {

    boolean existsByUserIdAndPartIdAndColorId(UUID userId, UUID partId, UUID colorId);

    @EntityGraph(attributePaths = {"part", "color"})
    Page<UserPart> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"part", "color"})
    Optional<UserPart> findByIdAndUserId(UUID id, UUID userId);
}

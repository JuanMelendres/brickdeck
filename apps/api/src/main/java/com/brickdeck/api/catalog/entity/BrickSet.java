package com.brickdeck.api.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "sets")
public class BrickSet {

    @Id
    private UUID id;

    @Column(name = "external_set_number", nullable = false, unique = true)
    private String externalSetNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "year_released")
    private Integer yearReleased;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Column(name = "external_theme_id")
    private Integer externalThemeId;

    @Column(name = "number_of_parts")
    private Integer numberOfParts;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "external_last_modified_at")
    private OffsetDateTime externalLastModifiedAt;

    @Column(nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (source == null || source.isBlank()) {
            source = "REBRICKABLE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
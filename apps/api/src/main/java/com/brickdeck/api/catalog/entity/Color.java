package com.brickdeck.api.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "colors")
public class Color {

    @Id
    private UUID id;

    @Column(name = "external_id", unique = true)
    private Integer externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "rgb")
    private String rgb;

    @Column(name = "is_transparent")
    private boolean transparent;

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

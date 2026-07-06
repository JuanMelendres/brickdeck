package com.brickdeck.api.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "parts")
public class Part {

    @Id
    private UUID id;

    @Column(name = "external_part_number", nullable = false, unique = true)
    private String externalPartNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "external_category_id")
    private Integer externalCategoryId;

    @Column(name = "part_url")
    private String partUrl;

    @Column(name = "image_url")
    private String imageUrl;

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

package com.brickdeck.api.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single inventory line of a set: a quantity of one part in one color,
 * optionally a spare. Normalized join between {@link BrickSet}, {@link Part}, and {@link Color}.
 */
@Getter
@Setter
@Entity
@Table(
        name = "set_parts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_set_parts_line",
                columnNames = {"set_id", "part_id", "color_id", "is_spare"}
        )
)
public class SetPart {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private BrickSet brickSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "is_spare", nullable = false)
    private boolean spare;

    @Column(name = "external_element_id")
    private String externalElementId;

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

package com.brickdeck.api.pricing.entity;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One observed price for a set, submitted by a user. Source-agnostic: the
 * {@code source} discriminator reserves space for future automated sources
 * (e.g. a marketplace API) writing the same table.
 */
@Getter
@Setter
@Entity
@Table(name = "price_snapshots")
public class PriceSnapshot {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private BrickSet brickSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PriceSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PriceCondition condition;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column
    private String store;

    @Column
    private String url;

    @Column(name = "observed_at", nullable = false)
    private LocalDate observedAt;

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
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

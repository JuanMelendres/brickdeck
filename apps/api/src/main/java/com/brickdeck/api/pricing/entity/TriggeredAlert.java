package com.brickdeck.api.pricing.entity;

import com.brickdeck.api.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A recorded firing of a {@link PriceAlertRule} against a specific
 * {@link PriceSnapshot}. {@code user} is denormalized from the rule for
 * owner-scoped queries without an extra join.
 */
@Getter
@Setter
@Entity
@Table(name = "triggered_alerts")
public class TriggeredAlert {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private PriceAlertRule rule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PriceSnapshot snapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
    }
}

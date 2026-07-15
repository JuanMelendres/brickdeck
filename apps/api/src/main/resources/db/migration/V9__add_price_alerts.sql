CREATE TABLE price_alert_rules (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    set_id UUID NOT NULL REFERENCES sets (id),
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(30) NOT NULL,
    threshold_value NUMERIC(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_price_alert_rules_user_set_currency
    ON price_alert_rules (user_id, set_id, currency);

CREATE TABLE triggered_alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    rule_id UUID NOT NULL REFERENCES price_alert_rules (id) ON DELETE CASCADE,
    snapshot_id UUID NOT NULL REFERENCES price_snapshots (id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    message VARCHAR(500) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_triggered_alerts_user ON triggered_alerts (user_id);

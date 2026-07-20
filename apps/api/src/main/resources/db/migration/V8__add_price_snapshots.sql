CREATE TABLE price_snapshots (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES users (id),
                                 set_id UUID NOT NULL REFERENCES sets (id),
                                 source VARCHAR(30) NOT NULL,
                                 condition VARCHAR(10) NOT NULL,
                                 currency VARCHAR(3) NOT NULL,
                                 amount NUMERIC(12, 2) NOT NULL,
                                 store VARCHAR(255),
                                 url VARCHAR(1024),
                                 observed_at DATE NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT chk_price_snapshot_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_price_snapshots_user_set ON price_snapshots (user_id, set_id);
CREATE INDEX idx_price_snapshots_user_set_currency ON price_snapshots (user_id, set_id, currency);

CREATE TABLE user_sets (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL REFERENCES users (id),
                           set_id UUID NOT NULL REFERENCES sets (id),
                           status VARCHAR(50) NOT NULL DEFAULT 'OWNED',
                           purchase_price NUMERIC(10, 2),
                           purchase_date DATE,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT uq_user_set UNIQUE (user_id, set_id)
);

CREATE INDEX idx_user_sets_user ON user_sets (user_id);

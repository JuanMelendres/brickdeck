CREATE TABLE user_parts (
                            id UUID PRIMARY KEY,
                            user_id UUID NOT NULL REFERENCES users (id),
                            part_id UUID NOT NULL REFERENCES parts (id),
                            color_id UUID NOT NULL REFERENCES colors (id),
                            quantity INTEGER NOT NULL,
                            storage_location VARCHAR(255),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uq_user_part UNIQUE (user_id, part_id, color_id)
);

CREATE INDEX idx_user_parts_user ON user_parts (user_id);

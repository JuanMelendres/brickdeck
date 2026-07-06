CREATE TABLE colors (
                        id UUID PRIMARY KEY,
                        external_id INTEGER UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        rgb VARCHAR(6),
                        is_transparent BOOLEAN NOT NULL DEFAULT FALSE,
                        source VARCHAR(100) NOT NULL DEFAULT 'REBRICKABLE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parts (
                       id UUID PRIMARY KEY,
                       external_part_number VARCHAR(100) NOT NULL UNIQUE,
                       name VARCHAR(255) NOT NULL,
                       external_category_id INTEGER,
                       part_url TEXT,
                       image_url TEXT,
                       source VARCHAR(100) NOT NULL DEFAULT 'REBRICKABLE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE set_parts (
                           id UUID PRIMARY KEY,
                           set_id UUID NOT NULL,
                           part_id UUID NOT NULL,
                           color_id UUID NOT NULL,
                           quantity INTEGER NOT NULL,
                           is_spare BOOLEAN NOT NULL DEFAULT FALSE,
                           external_element_id VARCHAR(100),
                           source VARCHAR(100) NOT NULL DEFAULT 'REBRICKABLE',
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_set_parts_set
                               FOREIGN KEY (set_id) REFERENCES sets(id),
                           CONSTRAINT fk_set_parts_part
                               FOREIGN KEY (part_id) REFERENCES parts(id),
                           CONSTRAINT fk_set_parts_color
                               FOREIGN KEY (color_id) REFERENCES colors(id),
                           CONSTRAINT uq_set_parts_line
                               UNIQUE (set_id, part_id, color_id, is_spare)
);

CREATE INDEX idx_set_parts_set_id ON set_parts (set_id);

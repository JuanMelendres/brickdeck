CREATE TABLE themes (
                        id UUID PRIMARY KEY,
                        external_id VARCHAR(100),
                        name VARCHAR(255) NOT NULL,
                        parent_theme_id UUID,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sets (
                      id UUID PRIMARY KEY,
                      external_set_number VARCHAR(100) NOT NULL UNIQUE,
                      name VARCHAR(255) NOT NULL,
                      year_released INTEGER,
                      theme_id UUID,
                      number_of_parts INTEGER,
                      image_url TEXT,
                      source VARCHAR(100) NOT NULL DEFAULT 'REBRICKABLE',
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                      CONSTRAINT fk_sets_theme
                          FOREIGN KEY (theme_id)
                              REFERENCES themes(id)
);
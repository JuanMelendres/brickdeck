ALTER TABLE sets
    ADD COLUMN external_theme_id INTEGER,
    ADD COLUMN external_url TEXT,
    ADD COLUMN external_last_modified_at TIMESTAMP WITH TIME ZONE;
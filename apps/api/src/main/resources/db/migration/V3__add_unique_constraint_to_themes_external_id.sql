ALTER TABLE themes
    ADD CONSTRAINT uq_themes_external_id UNIQUE (external_id);

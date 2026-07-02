package com.brickdeck.api.catalog.repository;

import com.brickdeck.api.catalog.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {
}
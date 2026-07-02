package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.service.ThemeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping("/{id}")
    public ThemeResponse getById(@PathVariable UUID id) {
        return themeService.getById(id);
    }
}

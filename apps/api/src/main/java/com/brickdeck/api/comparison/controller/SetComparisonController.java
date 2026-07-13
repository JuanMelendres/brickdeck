package com.brickdeck.api.comparison.controller;

import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sets")
public class SetComparisonController {

    private final SetComparisonService setComparisonService;

    public SetComparisonController(SetComparisonService setComparisonService) {
        this.setComparisonService = setComparisonService;
    }

    @GetMapping("/compare")
    public SetComparisonReport compare(
            @RequestParam("a") String a,
            @RequestParam("b") String b,
            @RequestParam(name = "category", required = false) ComparisonCategory category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return setComparisonService.compare(a, b, category, page, size);
    }
}

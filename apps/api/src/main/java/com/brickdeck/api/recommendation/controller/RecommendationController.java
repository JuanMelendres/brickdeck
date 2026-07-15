package com.brickdeck.api.recommendation.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.recommendation.dto.BuildableSetRecommendation;
import com.brickdeck.api.recommendation.service.RecommendationService;
import com.brickdeck.api.security.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Wishlist sets scored by how buildable they are from the user's owned
     * inventory, most-complete first. {@code buildableOnly} keeps only sets the
     * user can build right now.
     */
    @GetMapping("/buildable")
    public PageResponse<BuildableSetRecommendation> buildable(
            @RequestParam(name = "buildableOnly", defaultValue = "false") boolean buildableOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return recommendationService.recommendBuildable(user.getId(), buildableOnly, pageable);
    }
}

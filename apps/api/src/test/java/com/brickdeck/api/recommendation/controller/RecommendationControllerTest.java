package com.brickdeck.api.recommendation.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.recommendation.dto.BuildableSetRecommendation;
import com.brickdeck.api.recommendation.service.RecommendationService;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void returnsBuildableRecommendationsForAuthenticatedUser() throws Exception {
        PageResponse<BuildableSetRecommendation> page = PageResponse.of(
                List.of(new BuildableSetRecommendation(
                        "100-1", "Small Set", "City", 2, 2, 0, 100.0, true)),
                0, 20, 1);
        when(recommendationService.recommendBuildable(
                any(UUID.class), anyBoolean(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/recommendations/buildable")
                        .with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].setNumber").value("100-1"))
                .andExpect(jsonPath("$.content[0].buildable").value(true))
                .andExpect(jsonPath("$.content[0].completionPercentage").value(100.0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void forwardsBuildableOnlyFlag() throws Exception {
        when(recommendationService.recommendBuildable(
                any(UUID.class), eq(true), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/recommendations/buildable")
                        .param("buildableOnly", "true")
                        .with(authentication(principal())))
                .andExpect(status().isOk());

        verify(recommendationService).recommendBuildable(
                any(UUID.class), eq(true), any(Pageable.class));
    }
}

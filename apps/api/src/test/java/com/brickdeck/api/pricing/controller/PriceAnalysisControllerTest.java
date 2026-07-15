package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.pricing.dto.CandidateEvaluation;
import com.brickdeck.api.pricing.dto.DealVerdict;
import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.service.PriceAnalysisService;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceAnalysisController.class)
class PriceAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceAnalysisService priceAnalysisService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void returnsAnalysisWithACandidateVerdict() throws Exception {
        PriceAnalysisResponse response = new PriceAnalysisResponse(
                "75257-1", "USD", 3,
                new BigDecimal("80"), new BigDecimal("100.00"), new BigDecimal("120"),
                new BigDecimal("120"), 100, new BigDecimal("1.00"),
                new CandidateEvaluation(new BigDecimal("80"), new BigDecimal("0.80"),
                        new BigDecimal("20.0"), true, DealVerdict.GREAT_DEAL));
        when(priceAnalysisService.analyze(any(UUID.class), eq("75257-1"), eq("USD"),
                eq(new BigDecimal("80")))).thenReturn(response);

        mockMvc.perform(get("/api/v1/sets/75257-1/price-analysis")
                        .param("currency", "USD").param("candidatePrice", "80")
                        .with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageAmount").value(100.00))
                .andExpect(jsonPath("$.candidate.verdict").value("GREAT_DEAL"))
                .andExpect(jsonPath("$.candidate.atOrBelowLowest").value(true));
    }

    @Test
    void returnsAnalysisWithoutACandidateWhenNoCandidatePrice() throws Exception {
        PriceAnalysisResponse response = new PriceAnalysisResponse(
                "75257-1", "USD", 3, new BigDecimal("80"), new BigDecimal("100.00"),
                new BigDecimal("120"), new BigDecimal("120"), 100, new BigDecimal("1.00"), null);
        when(priceAnalysisService.analyze(any(UUID.class), eq("75257-1"), eq("USD"), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/sets/75257-1/price-analysis")
                        .param("currency", "USD").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidate").doesNotExist());
    }

    @Test
    void requiresACurrencyParam() throws Exception {
        mockMvc.perform(get("/api/v1/sets/75257-1/price-analysis")
                        .with(authentication(principal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenNoSnapshots() throws Exception {
        when(priceAnalysisService.analyze(any(UUID.class), eq("999-1"), eq("USD"), isNull()))
                .thenThrow(new ResourceNotFoundException("No price snapshots for set 999-1 in USD"));

        mockMvc.perform(get("/api/v1/sets/999-1/price-analysis")
                        .param("currency", "USD").with(authentication(principal())))
                .andExpect(status().isNotFound());
    }
}

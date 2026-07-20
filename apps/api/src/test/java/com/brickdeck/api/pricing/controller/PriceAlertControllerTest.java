package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.pricing.dto.PriceAlertRuleResponse;
import com.brickdeck.api.pricing.dto.TriggeredAlertResponse;
import com.brickdeck.api.pricing.entity.PriceAlertType;
import com.brickdeck.api.pricing.service.PriceAlertService;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceAlertController.class)
class PriceAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceAlertService priceAlertService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private PriceAlertRuleResponse rule() {
        return new PriceAlertRuleResponse(
                UUID.randomUUID(), "75257-1", "USD", PriceAlertType.BELOW_TARGET_PRICE,
                new BigDecimal("100.00"), true, LocalDateTime.now());
    }

    private TriggeredAlertResponse triggered() {
        return new TriggeredAlertResponse(
                UUID.randomUUID(), UUID.randomUUID(), "75257-1", new BigDecimal("80.00"),
                "USD", "80.00 is below your target 100.00", LocalDateTime.now());
    }

    @Test
    void createsARuleAndReturns201WithLocation() throws Exception {
        when(priceAlertService.createRule(any(), any())).thenReturn(rule());

        String body = """
                { "setNumber": "75257-1", "currency": "USD",
                  "type": "BELOW_TARGET_PRICE", "thresholdValue": 100.00 }
                """;

        mockMvc.perform(post("/api/v1/price-alerts")
                        .with(authentication(principal())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/price-alerts/")))
                .andExpect(jsonPath("$.type").value("BELOW_TARGET_PRICE"));
    }

    @Test
    void returns400WhenTheServiceRejectsTheThreshold() throws Exception {
        when(priceAlertService.createRule(any(), any()))
                .thenThrow(new IllegalArgumentException("thresholdValue must be a positive amount"));

        String body = """
                { "setNumber": "75257-1", "currency": "USD", "type": "BELOW_TARGET_PRICE" }
                """;

        mockMvc.perform(post("/api/v1/price-alerts")
                        .with(authentication(principal())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("thresholdValue must be a positive amount"));
    }

    @Test
    void listsRulesPaginated() throws Exception {
        when(priceAlertService.listRules(any(), any()))
                .thenReturn(PageResponse.of(List.of(rule()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/price-alerts").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].setNumber").value("75257-1"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listsTriggeredAlertsPaginated() throws Exception {
        when(priceAlertService.listTriggered(any(), any()))
                .thenReturn(PageResponse.of(List.of(triggered()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/price-alerts/triggered").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message")
                        .value("80.00 is below your target 100.00"));
    }

    @Test
    void deletesARule() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/price-alerts/{id}", id)
                        .with(authentication(principal())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(priceAlertService).deleteRule(any(), eq(id));
    }

    @Test
    void dismissesATriggeredAlert() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/price-alerts/triggered/{id}", id)
                        .with(authentication(principal())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(priceAlertService).deleteTriggered(any(), eq(id));
    }
}

package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.pricing.dto.PriceSnapshotResponse;
import com.brickdeck.api.pricing.entity.PriceCondition;
import com.brickdeck.api.pricing.entity.PriceSource;
import com.brickdeck.api.pricing.service.PriceSnapshotService;
import com.brickdeck.api.security.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(PriceSnapshotController.class)
class PriceSnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PriceSnapshotService priceSnapshotService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private PriceSnapshotResponse sample() {
        return new PriceSnapshotResponse(
                UUID.randomUUID(), "75257-1", new BigDecimal("129.99"), "USD",
                PriceCondition.NEW, PriceSource.MANUAL, LocalDate.of(2026, 1, 10),
                "LEGO Store", null, LocalDateTime.now());
    }

    @Test
    void createsASnapshotAndReturns201WithLocation() throws Exception {
        when(priceSnapshotService.addSnapshot(any(), any())).thenReturn(sample());

        String body = """
                { "setNumber": "75257-1", "amount": 129.99, "currency": "USD",
                  "condition": "NEW", "observedAt": "2026-01-10", "store": "LEGO Store" }
                """;

        mockMvc.perform(post("/api/v1/price-snapshots")
                        .with(authentication(principal())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/price-snapshots/")))
                .andExpect(jsonPath("$.setNumber").value("75257-1"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void rejectsANonPositiveAmount() throws Exception {
        String body = """
                { "setNumber": "75257-1", "amount": -1, "currency": "USD",
                  "condition": "NEW", "observedAt": "2026-01-10" }
                """;

        mockMvc.perform(post("/api/v1/price-snapshots")
                        .with(authentication(principal())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsABadCurrencyCode() throws Exception {
        String body = """
                { "setNumber": "75257-1", "amount": 10, "currency": "dollars",
                  "condition": "NEW", "observedAt": "2026-01-10" }
                """;

        mockMvc.perform(post("/api/v1/price-snapshots")
                        .with(authentication(principal())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsSnapshotsPaginated() throws Exception {
        when(priceSnapshotService.findForUser(any(), isNull(), any()))
                .thenReturn(PageResponse.of(List.of(sample()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/price-snapshots").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].setNumber").value("75257-1"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void filtersSnapshotsBySetNumber() throws Exception {
        when(priceSnapshotService.findForUser(any(), eq("75257-1"), any()))
                .thenReturn(PageResponse.of(List.of(sample()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/price-snapshots").param("setNumber", "75257-1")
                        .with(authentication(principal())))
                .andExpect(status().isOk());

        verify(priceSnapshotService).findForUser(any(), eq("75257-1"), any());
    }

    @Test
    void deletesASnapshot() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/price-snapshots/{id}", id)
                        .with(authentication(principal())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(priceSnapshotService).removeSnapshot(any(), eq(id));
    }
}

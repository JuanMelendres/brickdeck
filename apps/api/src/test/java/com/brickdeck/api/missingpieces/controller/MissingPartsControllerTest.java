package com.brickdeck.api.missingpieces.controller;

import com.brickdeck.api.common.ResourceNotFoundException;
import com.brickdeck.api.missingpieces.dto.MissingPartLine;
import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.service.MissingPartsService;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MissingPartsController.class)
class MissingPartsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MissingPartsService missingPartsService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void returnsMissingPartsReportForAuthenticatedUser() throws Exception {
        MissingPartsReport report = new MissingPartsReport(
                "75257-1", 6, 4, 2, 66.7, 1,
                List.of(new MissingPartLine(
                        "3001", "Brick 2 x 4", null, 4, "Red", "B40000", 4, 2, 2)),
                0, 50, 1L, 1, true, true);
        when(missingPartsService.computeMissingParts(
                eq("75257-1"), any(UUID.class), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/75257-1/missing-parts").with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value("75257-1"))
                .andExpect(jsonPath("$.totalRequired").value(6))
                .andExpect(jsonPath("$.totalMissing").value(2))
                .andExpect(jsonPath("$.completionPercentage").value(66.7))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lines[0].partNumber").value("3001"))
                .andExpect(jsonPath("$.lines[0].missing").value(2));
    }

    @Test
    void forwardsMissingOnlyAndPaginationParams() throws Exception {
        MissingPartsReport report = new MissingPartsReport(
                "75257-1", 6, 4, 2, 66.7, 1, List.of(), 2, 10, 1L, 1, false, true);
        when(missingPartsService.computeMissingParts(
                eq("75257-1"), any(UUID.class), eq(true), eq(2), eq(10)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/75257-1/missing-parts")
                        .param("missingOnly", "true")
                        .param("page", "2")
                        .param("size", "10")
                        .with(authentication(principal())))
                .andExpect(status().isOk());

        verify(missingPartsService).computeMissingParts(
                eq("75257-1"), any(UUID.class), eq(true), eq(2), eq(10));
    }

    @Test
    void returns404WhenSetOrInventoryMissing() throws Exception {
        when(missingPartsService.computeMissingParts(
                eq("999-1"), any(UUID.class), anyBoolean(), anyInt(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Set not imported: 999-1"));

        mockMvc.perform(get("/api/v1/sets/999-1/missing-parts").with(authentication(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not imported: 999-1"));
    }
}

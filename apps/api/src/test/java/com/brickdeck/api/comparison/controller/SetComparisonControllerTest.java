package com.brickdeck.api.comparison.controller;

import com.brickdeck.api.comparison.dto.ComparisonCategory;
import com.brickdeck.api.comparison.dto.SetComparisonLine;
import com.brickdeck.api.comparison.dto.SetComparisonReport;
import com.brickdeck.api.comparison.service.SetComparisonService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class SetComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetComparisonService setComparisonService;

    @Test
    void returnsComparisonReport() throws Exception {
        SetComparisonReport report = new SetComparisonReport(
                "A-1", "B-1", 0.17, 1, 1, 1,
                List.of(new SetComparisonLine(
                        "X", "Brick X", null, 1, "Red", "B40000", 4, 2, 2,
                        ComparisonCategory.BOTH)),
                0, 50, 3L, 1, true, true);
        when(setComparisonService.compare(eq("A-1"), eq("B-1"), isNull(), eq(0), eq(50)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/compare").param("a", "A-1").param("b", "B-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumberA").value("A-1"))
                .andExpect(jsonPath("$.setNumberB").value("B-1"))
                .andExpect(jsonPath("$.similarityScore").value(0.17))
                .andExpect(jsonPath("$.sharedLineCount").value(1))
                .andExpect(jsonPath("$.lines[0].partNumber").value("X"))
                .andExpect(jsonPath("$.lines[0].category").value("BOTH"));
    }

    @Test
    void forwardsCategoryAndPaginationParams() throws Exception {
        SetComparisonReport report = new SetComparisonReport(
                "A-1", "B-1", 0.0, 0, 1, 0, List.of(), 2, 10, 1L, 1, false, true);
        when(setComparisonService.compare(
                eq("A-1"), eq("B-1"), eq(ComparisonCategory.ONLY_A), eq(2), eq(10)))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/sets/compare")
                        .param("a", "A-1").param("b", "B-1")
                        .param("category", "ONLY_A")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(setComparisonService).compare(
                eq("A-1"), eq("B-1"), eq(ComparisonCategory.ONLY_A), eq(2), eq(10));
    }

    @Test
    void returns404WhenSetOrInventoryMissing() throws Exception {
        when(setComparisonService.compare(eq("A-1"), eq("B-1"), isNull(), eq(0), eq(50)))
                .thenThrow(new ResourceNotFoundException("Set not imported: A-1"));

        mockMvc.perform(get("/api/v1/sets/compare").param("a", "A-1").param("b", "B-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not imported: A-1"));
    }
}

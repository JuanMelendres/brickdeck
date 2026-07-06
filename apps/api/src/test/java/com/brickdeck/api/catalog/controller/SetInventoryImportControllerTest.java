package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.InventoryImportResult;
import com.brickdeck.api.catalog.service.SetInventoryService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetInventoryImportController.class)
class SetInventoryImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetInventoryService setInventoryService;

    @Test
    void importReturnsOkWithSummary() throws Exception {
        when(setInventoryService.importInventory("75375-1"))
                .thenReturn(new InventoryImportResult("75375-1", 42));

        mockMvc.perform(post("/api/v1/catalog/sets/{setNumber}/inventory/import", "75375-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value("75375-1"))
                .andExpect(jsonPath("$.linesProcessed").value(42));
    }

    @Test
    void importReturnsNotFoundWhenSetNotImported() throws Exception {
        when(setInventoryService.importInventory("00000-1"))
                .thenThrow(new ResourceNotFoundException("Set not imported: 00000-1 (import the set first)"));

        mockMvc.perform(post("/api/v1/catalog/sets/{setNumber}/inventory/import", "00000-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not imported: 00000-1 (import the set first)"));
    }
}

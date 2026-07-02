package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrickSetController.class)
class BrickSetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrickSetService brickSetService;

    @Test
    void returnsSetWithOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(brickSetService.getByExternalSetNumber("75257-1")).thenReturn(
                new BrickSetResponse(id, "75257-1", "Millennium Falcon", 2019,
                        UUID.randomUUID(), "Star Wars", 1351, "https://img", "REBRICKABLE"));

        mockMvc.perform(get("/api/catalog/sets/{setNumber}", "75257-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalSetNumber").value("75257-1"))
                .andExpect(jsonPath("$.name").value("Millennium Falcon"))
                .andExpect(jsonPath("$.themeName").value("Star Wars"));
    }

    @Test
    void returnsNotFoundWhenMissing() throws Exception {
        when(brickSetService.getByExternalSetNumber("nope"))
                .thenThrow(new ResourceNotFoundException("Set not found: nope"));

        mockMvc.perform(get("/api/catalog/sets/{setNumber}", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not found: nope"));
    }
}

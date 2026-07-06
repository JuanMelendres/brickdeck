package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.service.BrickSetService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrickSetImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrickSetImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrickSetService brickSetService;

    private BrickSetResponse falconResponse(String cacheStatus) {
        return new BrickSetResponse(
                UUID.randomUUID(),
                "75375-1",
                "Millennium Falcon",
                2024,
                null,
                null,
                158,
                921,
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                "https://rebrickable.com/sets/75375-1/millennium-falcon/",
                "REBRICKABLE",
                cacheStatus
        );
    }

    @Test
    void importReturnsCreatedForNewSet() throws Exception {
        when(brickSetService.importSet("75375-1"))
                .thenReturn(new ImportResult(true, falconResponse("IMPORTED_FROM_REBRICKABLE")));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"75375-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalSetNumber").value("75375-1"))
                .andExpect(jsonPath("$.cacheStatus").value("IMPORTED_FROM_REBRICKABLE"));
    }

    @Test
    void importReturnsOkForExistingSet() throws Exception {
        when(brickSetService.importSet("75375-1"))
                .thenReturn(new ImportResult(false, falconResponse("REFRESHED_FROM_REBRICKABLE")));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"75375-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheStatus").value("REFRESHED_FROM_REBRICKABLE"));
    }

    @Test
    void importReturnsBadRequestWhenSetNumberBlank() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(brickSetService, never()).importSet(any());
    }

    @Test
    void importReturnsNotFoundWhenSetMissingInRebrickable() throws Exception {
        when(brickSetService.importSet("99999-1"))
                .thenThrow(new ResourceNotFoundException("Set not found in Rebrickable: 99999-1"));

        mockMvc.perform(post("/api/v1/catalog/sets/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"99999-1\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Set not found in Rebrickable: 99999-1"));
    }
}

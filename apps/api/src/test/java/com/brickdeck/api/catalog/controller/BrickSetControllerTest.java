package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.service.BrickSetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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
    void returnsAllSetsWithOk() throws Exception {
        UUID id = UUID.randomUUID();

        BrickSetResponse response = new BrickSetResponse(
                id,
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
                "LOCAL_CACHE_HIT"
        );

        when(brickSetService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].externalSetNumber").value("75375-1"))
                .andExpect(jsonPath("$[0].name").value("Millennium Falcon"))
                .andExpect(jsonPath("$[0].yearReleased").value(2024))
                .andExpect(jsonPath("$[0].themeId").doesNotExist())
                .andExpect(jsonPath("$[0].themeName").doesNotExist())
                .andExpect(jsonPath("$[0].externalThemeId").value(158))
                .andExpect(jsonPath("$[0].numberOfParts").value(921))
                .andExpect(jsonPath("$[0].source").value("REBRICKABLE"))
                .andExpect(jsonPath("$[0].cacheStatus").value("LOCAL_CACHE_HIT"));
    }

    @Test
    void returnsSetByNumberWithOk() throws Exception {
        UUID id = UUID.randomUUID();

        BrickSetResponse response = new BrickSetResponse(
                id,
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
                "IMPORTED_FROM_REBRICKABLE"
        );

        when(brickSetService.findOrImportBySetNumber("75375-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/sets/by-number/{setNumber}", "75375-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.externalSetNumber").value("75375-1"))
                .andExpect(jsonPath("$.name").value("Millennium Falcon"))
                .andExpect(jsonPath("$.yearReleased").value(2024))
                .andExpect(jsonPath("$.themeId").doesNotExist())
                .andExpect(jsonPath("$.themeName").doesNotExist())
                .andExpect(jsonPath("$.externalThemeId").value(158))
                .andExpect(jsonPath("$.numberOfParts").value(921))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg"))
                .andExpect(jsonPath("$.externalUrl").value("https://rebrickable.com/sets/75375-1/millennium-falcon/"))
                .andExpect(jsonPath("$.source").value("REBRICKABLE"))
                .andExpect(jsonPath("$.cacheStatus").value("IMPORTED_FROM_REBRICKABLE"));
    }
}

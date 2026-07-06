package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.ThemeResponse;
import com.brickdeck.api.catalog.service.ThemeService;
import com.brickdeck.api.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ThemeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ThemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThemeService themeService;

    @Test
    void returnsThemeWithOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(themeService.getById(id)).thenReturn(
                new ThemeResponse(id, "158", "Star Wars", null));

        mockMvc.perform(get("/api/catalog/themes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Star Wars"));
    }

    @Test
    void returnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(themeService.getById(id))
                .thenThrow(new ResourceNotFoundException("Theme not found: " + id));

        mockMvc.perform(get("/api/catalog/themes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Theme not found: " + id));
    }

    @Test
    void returnsBadRequestWhenIdNotUuid() throws Exception {
        mockMvc.perform(get("/api/catalog/themes/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}

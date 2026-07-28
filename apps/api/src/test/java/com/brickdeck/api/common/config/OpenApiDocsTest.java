package com.brickdeck.api.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAreExposedWithCatalogPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("BrickDeck Catalog API"))
                .andExpect(jsonPath("$.paths['/api/v1/sets']").exists());
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void brickSetResponseMarksGenuinelyNullableFieldsOnly() throws Exception {
        String base = "$.components.schemas.BrickSetResponse.properties.";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Absent for a search result (not yet imported) or before a theme resolves.
                .andExpect(jsonPath(base + "id.nullable").value(true))
                .andExpect(jsonPath(base + "yearReleased.nullable").value(true))
                .andExpect(jsonPath(base + "themeId.nullable").value(true))
                .andExpect(jsonPath(base + "themeName.nullable").value(true))
                .andExpect(jsonPath(base + "externalThemeId.nullable").value(true))
                .andExpect(jsonPath(base + "numberOfParts.nullable").value(true))
                .andExpect(jsonPath(base + "imageUrl.nullable").value(true))
                .andExpect(jsonPath(base + "externalUrl.nullable").value(true))
                // Always populated, backed by a NOT NULL column or a literal.
                .andExpect(jsonPath(base + "externalSetNumber.nullable").doesNotExist())
                .andExpect(jsonPath(base + "name.nullable").doesNotExist())
                .andExpect(jsonPath(base + "source.nullable").doesNotExist())
                .andExpect(jsonPath(base + "cacheStatus.nullable").doesNotExist());
    }

    @Test
    void setPartResponseMarksGenuinelyNullableFieldsOnly() throws Exception {
        String base = "$.components.schemas.SetPartResponse.properties.";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(base + "partImageUrl.nullable").value(true))
                .andExpect(jsonPath(base + "colorExternalId.nullable").value(true))
                .andExpect(jsonPath(base + "colorRgb.nullable").value(true))
                .andExpect(jsonPath(base + "elementId.nullable").value(true))
                .andExpect(jsonPath(base + "id.nullable").doesNotExist())
                .andExpect(jsonPath(base + "setNumber.nullable").doesNotExist())
                .andExpect(jsonPath(base + "partNumber.nullable").doesNotExist())
                .andExpect(jsonPath(base + "partName.nullable").doesNotExist())
                .andExpect(jsonPath(base + "colorName.nullable").doesNotExist())
                .andExpect(jsonPath(base + "quantity.nullable").doesNotExist());
    }
}

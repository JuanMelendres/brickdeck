package com.brickdeck.api.classification;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.brickdeck.api.external.brickognize.client.BrickognizeClient;
import com.brickdeck.api.external.brickognize.dto.BrickognizeItem;
import com.brickdeck.api.external.brickognize.dto.BrickognizePredictResponse;
import com.brickdeck.api.external.gemini.client.GeminiClient;
import com.brickdeck.api.external.gemini.dto.GeminiColorGuess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real Spring context + real Postgres (needs `localhost:5433` up). The two vendor
 * adapters are mocked here — this proves security/wiring/merge end-to-end without
 * making live Brickognize/Gemini calls in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PartClassificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ColorRepository colorRepository;

    @MockitoBean
    private BrickognizeClient brickognizeClient;

    @MockitoBean
    private GeminiClient geminiClient;

    // Synthetic catalog refs, unlikely to collide with real imported data in the shared DB.
    private static final String PART_NUMBER = "IT-CLASSIFY-3001";
    private static final int COLOR_ID = 999_101;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        Part part = new Part();
        part.setExternalPartNumber(PART_NUMBER);
        part.setName("Brick 2 x 4");
        part.setImageUrl("https://cdn.rebrickable.com/media/parts/elements/300121.jpg");
        partRepository.save(part);

        Color color = new Color();
        color.setExternalId(COLOR_ID);
        color.setName("Test Red");
        color.setRgb("C91A09");
        colorRepository.save(color);

        token = register("classify-user@brickdeck.test", "secret123");
    }

    @Test
    void classifyPartRequiresAuthentication() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/classify/part").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void classifyPartReturnsMergedPartAndColorSuggestions() throws Exception {
        when(brickognizeClient.identifyPart(any())).thenReturn(new BrickognizePredictResponse(List.of(
                new BrickognizeItem(PART_NUMBER, "Brick 2 x 4", 0.84, "Brick", null)
        )));
        when(geminiClient.guessColor(any(), any(), anyList()))
                .thenReturn(new GeminiColorGuess("Test Red", 0.95, "solid red"));

        MockMultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/classify/part").file(image)
                        .header("Authorization", "Bearer " + token)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colorSuggestion.colorId").value(COLOR_ID))
                .andExpect(jsonPath("$.colorSuggestion.colorName").value("Test Red"))
                .andExpect(jsonPath("$.partSuggestions[0].partNumber").value(PART_NUMBER))
                .andExpect(jsonPath("$.partSuggestions[0].resolutionStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.partSuggestions[0].referenceImageUrl")
                        .value("https://cdn.rebrickable.com/media/parts/elements/300121.jpg"));
    }

    private String register(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Classify User"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }
}

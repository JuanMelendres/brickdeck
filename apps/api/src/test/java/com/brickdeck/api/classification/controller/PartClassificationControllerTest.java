package com.brickdeck.api.classification.controller;

import com.brickdeck.api.classification.PartClassifier;
import com.brickdeck.api.classification.dto.ColorSuggestion;
import com.brickdeck.api.classification.dto.PartClassificationResponse;
import com.brickdeck.api.classification.dto.PartResolutionStatus;
import com.brickdeck.api.classification.dto.PartSuggestion;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartClassificationController.class)
class PartClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartClassifier partClassifier;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void classifiesUploadedPhotoForAuthenticatedUser() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        PartClassificationResponse response = new PartClassificationResponse(
                new ColorSuggestion(4, "Red", 0.95),
                List.of(new PartSuggestion("3001", "Brick 2 x 4", 0.84, PartResolutionStatus.RESOLVED,
                        "https://cdn.rebrickable.com/media/parts/elements/300121.jpg"))
        );
        when(partClassifier.classify(eq("fake-bytes".getBytes()), eq("image/jpeg"))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/classify/part").file(image)
                        .with(authentication(principal()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colorSuggestion.colorId").value(4))
                .andExpect(jsonPath("$.colorSuggestion.colorName").value("Red"))
                .andExpect(jsonPath("$.partSuggestions[0].partNumber").value("3001"))
                .andExpect(jsonPath("$.partSuggestions[0].resolutionStatus").value("RESOLVED"));
    }

    @Test
    void rejectsRequestMissingImagePart() throws Exception {
        mockMvc.perform(multipart("/api/v1/classify/part")
                        .with(authentication(principal()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

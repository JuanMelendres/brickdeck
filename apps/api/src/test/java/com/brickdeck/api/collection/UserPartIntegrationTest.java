package com.brickdeck.api.collection;

import com.brickdeck.api.catalog.entity.Color;
import com.brickdeck.api.catalog.entity.Part;
import com.brickdeck.api.catalog.repository.ColorRepository;
import com.brickdeck.api.catalog.repository.PartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ColorRepository colorRepository;

    private static final String PARTS = "/api/v1/collection/parts";
    // Synthetic catalog refs, unlikely to collide with real imported data in the shared DB.
    private static final String PART_NUMBER = "IT-PART-3001";
    private static final int COLOR_ID = 999_001;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        Part part = new Part();
        part.setExternalPartNumber(PART_NUMBER);
        part.setName("Brick 2 x 4");
        partRepository.save(part);

        Color color = new Color();
        color.setExternalId(COLOR_ID);
        color.setName("Test Red");
        color.setRgb("C91A09");
        colorRepository.save(color);

        token = register("looseparts-user@brickdeck.test", "secret123");
    }

    @Test
    void addPartRequiresAuthentication() throws Exception {
        mockMvc.perform(post(PARTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(3)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addPartReturns201AndAppearsInList() throws Exception {
        mockMvc.perform(post(PARTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(12)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partNumber").value(PART_NUMBER))
                .andExpect(jsonPath("$.colorExternalId").value(COLOR_ID))
                .andExpect(jsonPath("$.quantity").value(12));

        mockMvc.perform(get(PARTS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partNumber").value(PART_NUMBER))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void addPartRejectsUnknownPartWith404() throws Exception {
        mockMvc.perform(post(PARTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalPartNumber":"DOES-NOT-EXIST","colorExternalId":%d,"quantity":1}
                                """.formatted(COLOR_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addPartRejectsDuplicateWith409() throws Exception {
        mockMvc.perform(post(PARTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PARTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(1)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAndRemoveEntry() throws Exception {
        String id = addAndGetId();

        mockMvc.perform(patch(PARTS + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":50,"storageLocation":"Bin 7"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.storageLocation").value("Bin 7"));

        mockMvc.perform(delete(PARTS + "/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PARTS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cannotModifyAnotherUsersEntry() throws Exception {
        String id = addAndGetId();
        String otherToken = register("other-looseparts@brickdeck.test", "secret123");

        mockMvc.perform(delete(PARTS + "/" + id)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    private String addBody(int quantity) {
        return """
                {"externalPartNumber":"%s","colorExternalId":%d,"quantity":%d}
                """.formatted(PART_NUMBER, COLOR_ID, quantity);
    }

    private String addAndGetId() throws Exception {
        String body = mockMvc.perform(post(PARTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(5)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String register(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Loose Parts User"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }
}

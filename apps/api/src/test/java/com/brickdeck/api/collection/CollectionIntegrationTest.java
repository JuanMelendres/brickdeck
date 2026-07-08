package com.brickdeck.api.collection;

import com.brickdeck.api.catalog.entity.BrickSet;
import com.brickdeck.api.catalog.repository.BrickSetRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrickSetRepository brickSetRepository;

    private static final String COLLECTION = "/api/v1/collection/sets";
    // Synthetic number: not present in Rebrickable nor the shared catalog, so add-set
    // always resolves via the seeded local cache-hit path.
    private static final String SET_NUMBER = "IT-COLLECTION-1";

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        // Seed the catalog directly so add-set resolves via the local cache-hit path
        // (no Rebrickable call during the test).
        BrickSet set = new BrickSet();
        set.setExternalSetNumber(SET_NUMBER);
        set.setName("Millennium Falcon");
        set.setYearReleased(2024);
        brickSetRepository.save(set);

        token = register("collection-user@brickdeck.test", "secret123");
    }

    @Test
    void addSetRequiresAuthentication() throws Exception {
        mockMvc.perform(post(COLLECTION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":\"%s\"}".formatted(SET_NUMBER)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addSetReturns201AndAppearsInList() throws Exception {
        mockMvc.perform(post(COLLECTION)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setNumber":"%s","status":"BUILT","purchasePrice":849.99}
                                """.formatted(SET_NUMBER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(SET_NUMBER))
                .andExpect(jsonPath("$.setName").value("Millennium Falcon"))
                .andExpect(jsonPath("$.status").value("BUILT"));

        mockMvc.perform(get(COLLECTION)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].setNumber").value(SET_NUMBER))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void addSetRejectsDuplicateWith409() throws Exception {
        String body = "{\"setNumber\":\"%s\"}".formatted(SET_NUMBER);

        mockMvc.perform(post(COLLECTION)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(COLLECTION)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String register(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Collection User"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }
}

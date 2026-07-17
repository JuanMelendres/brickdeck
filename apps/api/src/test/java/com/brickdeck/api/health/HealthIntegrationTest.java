package com.brickdeck.api.health;

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
class HealthIntegrationTest {

    private static final String HEALTH = "/api/v1/health";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get(HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("brickdeck-api"))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}

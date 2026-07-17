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
    private static final String ACTUATOR_HEALTH = "/actuator/health";
    private static final String ACTUATOR_INFO = "/actuator/info";

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

    /**
     * Readiness probes must reach this without credentials. Authenticating it would
     * make it unreachable exactly when it matters: loading a user hits the database,
     * so a database outage returns 401 rather than DOWN.
     */
    @Test
    void actuatorHealthIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get(ACTUATOR_HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /** Only `status` is public; `show-details` stays at its `never` default. */
    @Test
    void actuatorHealthExposesNoDetails() throws Exception {
        mockMvc.perform(get(ACTUATOR_HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    /** Health is public; the rest of actuator is not. */
    @Test
    void actuatorInfoStaysAuthenticated() throws Exception {
        mockMvc.perform(get(ACTUATOR_INFO))
                .andExpect(status().isUnauthorized());
    }
}

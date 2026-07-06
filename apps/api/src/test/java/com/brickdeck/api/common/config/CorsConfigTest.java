package com.brickdeck.api.common.config;

import com.brickdeck.api.catalog.controller.BrickSetController;
import com.brickdeck.api.catalog.service.BrickSetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrickSetController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties = "brickdeck.cors.allowed-origins=http://localhost:3000")
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrickSetService brickSetService;

    @Test
    void preflightFromAllowedOriginIsPermitted() throws Exception {
        mockMvc.perform(options("/api/v1/sets")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void preflightFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/sets")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}

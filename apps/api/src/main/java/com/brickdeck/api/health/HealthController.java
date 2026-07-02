package com.brickdeck.api.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class HealthController {

    private static final String SERVICE_NAME = "brickdeck-api";
    private static final String VERSION = "0.1.0";

    @GetMapping("/api/v1/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse(
                "UP",
                SERVICE_NAME,
                VERSION,
                OffsetDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}
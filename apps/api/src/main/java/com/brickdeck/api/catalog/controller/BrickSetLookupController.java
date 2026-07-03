package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.external.rebrickable.client.RebrickableClient;
import com.brickdeck.api.external.rebrickable.dto.RebrickableSetResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/sets")
public class BrickSetLookupController {

    private final RebrickableClient rebrickableClient;

    public BrickSetLookupController(RebrickableClient rebrickableClient) {
        this.rebrickableClient = rebrickableClient;
    }

    @GetMapping("/external/{setNumber}")
    public ResponseEntity<RebrickableSetResponse> getExternalSet(@PathVariable String setNumber) {
        return ResponseEntity.ok(rebrickableClient.getSetByNumber(setNumber));
    }
}
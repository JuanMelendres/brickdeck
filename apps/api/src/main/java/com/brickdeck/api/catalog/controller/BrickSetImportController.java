package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.BrickSetResponse;
import com.brickdeck.api.catalog.dto.ImportResult;
import com.brickdeck.api.catalog.dto.ImportSetRequest;
import com.brickdeck.api.catalog.service.BrickSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/sets")
public class BrickSetImportController {

    private final BrickSetService brickSetService;

    public BrickSetImportController(BrickSetService brickSetService) {
        this.brickSetService = brickSetService;
    }

    @PostMapping("/import")
    public ResponseEntity<BrickSetResponse> importSet(@Valid @RequestBody ImportSetRequest request) {
        ImportResult result = brickSetService.importSet(request.setNumber());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.body());
    }
}

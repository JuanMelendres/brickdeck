package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.pricing.dto.AddPriceSnapshotRequest;
import com.brickdeck.api.pricing.dto.PriceSnapshotResponse;
import com.brickdeck.api.pricing.service.PriceSnapshotService;
import com.brickdeck.api.security.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/price-snapshots")
public class PriceSnapshotController {

    private final PriceSnapshotService priceSnapshotService;

    public PriceSnapshotController(PriceSnapshotService priceSnapshotService) {
        this.priceSnapshotService = priceSnapshotService;
    }

    @PostMapping
    public ResponseEntity<PriceSnapshotResponse> add(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddPriceSnapshotRequest request) {
        PriceSnapshotResponse response = priceSnapshotService.addSnapshot(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<PriceSnapshotResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "setNumber", required = false) String setNumber,
            @PageableDefault(size = 20, sort = "observedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return priceSnapshotService.findForUser(user, setNumber, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        priceSnapshotService.removeSnapshot(user, id);
        return ResponseEntity.noContent().build();
    }
}

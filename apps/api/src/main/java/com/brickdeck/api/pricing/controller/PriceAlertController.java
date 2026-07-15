package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.pricing.dto.AddPriceAlertRuleRequest;
import com.brickdeck.api.pricing.dto.PriceAlertRuleResponse;
import com.brickdeck.api.pricing.dto.TriggeredAlertResponse;
import com.brickdeck.api.pricing.service.PriceAlertService;
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
@RequestMapping("/api/v1/price-alerts")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    public PriceAlertController(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @PostMapping
    public ResponseEntity<PriceAlertRuleResponse> createRule(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddPriceAlertRuleRequest request) {
        PriceAlertRuleResponse response = priceAlertService.createRule(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<PriceAlertRuleResponse> listRules(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return priceAlertService.listRules(user, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        priceAlertService.deleteRule(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/triggered")
    public PageResponse<TriggeredAlertResponse> listTriggered(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "triggeredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return priceAlertService.listTriggered(user, pageable);
    }

    @DeleteMapping("/triggered/{id}")
    public ResponseEntity<Void> dismissTriggered(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        priceAlertService.deleteTriggered(user, id);
        return ResponseEntity.noContent().build();
    }
}

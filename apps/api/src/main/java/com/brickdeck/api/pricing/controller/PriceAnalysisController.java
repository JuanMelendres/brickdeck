package com.brickdeck.api.pricing.controller;

import com.brickdeck.api.pricing.dto.PriceAnalysisResponse;
import com.brickdeck.api.pricing.service.PriceAnalysisService;
import com.brickdeck.api.security.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/sets")
public class PriceAnalysisController {

    private final PriceAnalysisService priceAnalysisService;

    public PriceAnalysisController(PriceAnalysisService priceAnalysisService) {
        this.priceAnalysisService = priceAnalysisService;
    }

    @GetMapping("/{setNumber}/price-analysis")
    public PriceAnalysisResponse analyze(
            @AuthenticationPrincipal User user,
            @PathVariable String setNumber,
            @RequestParam("currency") String currency,
            @RequestParam(name = "candidatePrice", required = false) BigDecimal candidatePrice) {
        return priceAnalysisService.analyze(user.getId(), setNumber, currency, candidatePrice);
    }
}

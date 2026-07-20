package com.brickdeck.api.missingpieces.controller;

import com.brickdeck.api.missingpieces.dto.MissingPartsReport;
import com.brickdeck.api.missingpieces.service.MissingPartsService;
import com.brickdeck.api.security.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sets")
public class MissingPartsController {

    private final MissingPartsService missingPartsService;

    public MissingPartsController(MissingPartsService missingPartsService) {
        this.missingPartsService = missingPartsService;
    }

    @GetMapping("/{setNumber}/missing-parts")
    public MissingPartsReport getMissingParts(
            @PathVariable String setNumber,
            @RequestParam(name = "missingOnly", defaultValue = "false") boolean missingOnly,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        return missingPartsService.computeMissingParts(
                setNumber, user.getId(), missingOnly, page, size);
    }
}

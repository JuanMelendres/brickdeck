package com.brickdeck.api.collection.controller;

import com.brickdeck.api.collection.dto.AddUserPartRequest;
import com.brickdeck.api.collection.dto.UpdateUserPartRequest;
import com.brickdeck.api.collection.dto.UserPartResponse;
import com.brickdeck.api.collection.service.UserPartService;
import com.brickdeck.api.common.PageResponse;
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
@RequestMapping("/api/v1/collection/parts")
public class UserPartController {

    private final UserPartService userPartService;

    public UserPartController(UserPartService userPartService) {
        this.userPartService = userPartService;
    }

    @PostMapping
    public ResponseEntity<UserPartResponse> addPart(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody AddUserPartRequest request) {
        UserPartResponse response = userPartService.addPart(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PageResponse<UserPartResponse> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userPartService.findForUser(user, pageable);
    }

    @PatchMapping("/{id}")
    public UserPartResponse update(@AuthenticationPrincipal User user,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody UpdateUserPartRequest request) {
        return userPartService.updateEntry(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal User user,
                                       @PathVariable UUID id) {
        userPartService.removeEntry(user, id);
        return ResponseEntity.noContent().build();
    }
}

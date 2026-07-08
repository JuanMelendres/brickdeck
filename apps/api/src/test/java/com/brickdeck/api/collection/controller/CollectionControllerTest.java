package com.brickdeck.api.collection.controller;

import com.brickdeck.api.collection.dto.UserSetResponse;
import com.brickdeck.api.collection.entity.CollectionStatus;
import com.brickdeck.api.collection.service.CollectionService;
import com.brickdeck.api.common.PageResponse;
import com.brickdeck.api.security.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private UserSetResponse falconEntry() {
        return new UserSetResponse(
                UUID.randomUUID(),
                "75375-1",
                "Millennium Falcon",
                2024,
                "Star Wars",
                "https://cdn.rebrickable.com/media/sets/75375-1/136884.jpg",
                CollectionStatus.OWNED,
                new BigDecimal("849.99"),
                LocalDate.of(2024, 5, 1),
                LocalDateTime.now()
        );
    }

    @Test
    void addSetReturns201WithLocationAndBody() throws Exception {
        when(collectionService.addSet(any(User.class), any())).thenReturn(falconEntry());

        mockMvc.perform(post("/api/v1/collection/sets")
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setNumber":"75375-1","status":"OWNED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/collection/sets/")))
                .andExpect(jsonPath("$.setNumber").value("75375-1"))
                .andExpect(jsonPath("$.setName").value("Millennium Falcon"))
                .andExpect(jsonPath("$.status").value("OWNED"));
    }

    @Test
    void addSetRejectsBlankSetNumberWith400() throws Exception {
        mockMvc.perform(post("/api/v1/collection/sets")
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"setNumber":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.setNumber").exists());
    }

    @Test
    void listReturnsPageForCurrentUser() throws Exception {
        when(collectionService.findForUser(any(User.class), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(falconEntry()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/collection/sets")
                        .with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].setNumber").value("75375-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateReturns200WithUpdatedEntry() throws Exception {
        UUID id = UUID.randomUUID();
        when(collectionService.updateEntry(any(User.class), eq(id), any()))
                .thenReturn(falconEntry());

        mockMvc.perform(patch("/api/v1/collection/sets/{id}", id)
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BUILT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value("75375-1"));
    }

    @Test
    void removeReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/collection/sets/{id}", id)
                        .with(authentication(principal()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(collectionService).removeEntry(any(User.class), eq(id));
    }
}

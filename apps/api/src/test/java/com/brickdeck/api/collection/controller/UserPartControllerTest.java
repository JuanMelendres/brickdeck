package com.brickdeck.api.collection.controller;

import com.brickdeck.api.collection.dto.UserPartResponse;
import com.brickdeck.api.collection.service.UserPartService;
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

@WebMvcTest(UserPartController.class)
class UserPartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserPartService userPartService;

    private Authentication principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@brickdeck.test");
        user.setRole("USER");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private UserPartResponse brickEntry() {
        return new UserPartResponse(
                UUID.randomUUID(),
                "3001",
                "Brick 2 x 4",
                "https://cdn.rebrickable.com/media/parts/3001.jpg",
                4,
                "Red",
                "C91A09",
                12,
                "Drawer A3",
                LocalDateTime.now()
        );
    }

    @Test
    void addPartReturns201WithLocationAndBody() throws Exception {
        when(userPartService.addPart(any(User.class), any())).thenReturn(brickEntry());

        mockMvc.perform(post("/api/v1/collection/parts")
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalPartNumber":"3001","colorExternalId":4,"quantity":12,"storageLocation":"Drawer A3"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/collection/parts/")))
                .andExpect(jsonPath("$.partNumber").value("3001"))
                .andExpect(jsonPath("$.colorName").value("Red"))
                .andExpect(jsonPath("$.quantity").value(12));
    }

    @Test
    void addPartRejectsMissingFieldsWith400() throws Exception {
        mockMvc.perform(post("/api/v1/collection/parts")
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalPartNumber":"  ","colorExternalId":4,"quantity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.externalPartNumber").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    @Test
    void listReturnsPageForCurrentUser() throws Exception {
        when(userPartService.findForUser(any(User.class), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(brickEntry()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/collection/parts")
                        .with(authentication(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partNumber").value("3001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userPartService.updateEntry(any(User.class), eq(id), any())).thenReturn(brickEntry());

        mockMvc.perform(patch("/api/v1/collection/parts/{id}", id)
                        .with(authentication(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partNumber").value("3001"));
    }

    @Test
    void removeReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/collection/parts/{id}", id)
                        .with(authentication(principal()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userPartService).removeEntry(any(User.class), eq(id));
    }
}

package com.brickdeck.api.catalog.controller;

import com.brickdeck.api.catalog.dto.SetPartResponse;
import com.brickdeck.api.catalog.service.SetInventoryService;
import com.brickdeck.api.common.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetPartController.class)
@AutoConfigureMockMvc(addFilters = false)
class SetPartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetInventoryService setInventoryService;

    @Test
    void returnsInventoryPageWithOk() throws Exception {
        SetPartResponse response = new SetPartResponse(
                UUID.randomUUID(),
                "75375-1",
                "3001",
                "Brick 2 x 4",
                "https://cdn.rebrickable.com/media/parts/3001.jpg",
                4,
                "Red",
                "C91A09",
                6,
                false,
                "300121"
        );

        when(setInventoryService.findInventory(eq("75375-1"), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(response), 0, 50, 1));

        mockMvc.perform(get("/api/v1/sets/{setNumber}/parts", "75375-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partNumber").value("3001"))
                .andExpect(jsonPath("$.content[0].colorName").value("Red"))
                .andExpect(jsonPath("$.content[0].quantity").value(6))
                .andExpect(jsonPath("$.content[0].spare").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}

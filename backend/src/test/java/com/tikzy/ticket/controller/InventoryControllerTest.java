package com.tikzy.ticket.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.ticket.dto.response.InventoryResponse;
import com.tikzy.ticket.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void initialize_asOrganizer_returnsInventories() throws Exception {
        UUID eventId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .totalQuantity(50)
                .availableQuantity(50)
                .build();
        when(inventoryService.initialize(eq("organizer@example.com"), eq(eventId), any()))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/inventories/initialize", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "defaultTotalQuantity": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Khởi tạo tồn kho thành công"))
                .andExpect(jsonPath("$.data[0].totalQuantity").value(50));

        verify(inventoryService).initialize(eq("organizer@example.com"), eq(eventId), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void initialize_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/inventories/initialize", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(inventoryService, never()).initialize(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void upsert_asOrganizer_returnsUpdatedInventories() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .showTimeId(showTimeId)
                .ticketTypeId(ticketTypeId)
                .totalQuantity(120)
                .availableQuantity(120)
                .build();
        when(inventoryService.upsert(eq("organizer@example.com"), eq(eventId), any()))
                .thenReturn(List.of(response));

        mockMvc.perform(put("/api/v1/organizer/events/{eventId}/inventories", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "showTimeId": "%s",
                                      "ticketTypeId": "%s",
                                      "totalQuantity": 120
                                    }
                                  ]
                                }
                                """.formatted(showTimeId, ticketTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalQuantity").value(120));

        verify(inventoryService).upsert(eq("organizer@example.com"), eq(eventId), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void upsert_missingItems_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/organizer/events/{eventId}/inventories", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400));

        verify(inventoryService, never()).upsert(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwn_asOrganizer_returnsInventories() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .showTimeId(showTimeId)
                .ticketTypeName("VIP")
                .availableQuantity(80)
                .build();
        when(inventoryService.getOwn(eq("organizer@example.com"), eq(eventId), eq(showTimeId), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/organizer/events/{eventId}/inventories", eventId)
                        .param("showTimeId", showTimeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ticketTypeName").value("VIP"))
                .andExpect(jsonPath("$.data[0].availableQuantity").value(80));

        verify(inventoryService).getOwn(eq("organizer@example.com"), eq(eventId), eq(showTimeId), eq(null), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void update_asOrganizer_returnsUpdatedInventory() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(inventoryId)
                .eventId(eventId)
                .totalQuantity(200)
                .availableQuantity(200)
                .build();
        when(inventoryService.update(eq("organizer@example.com"), eq(eventId), eq(inventoryId), any()))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/organizer/events/{eventId}/inventories/{inventoryId}",
                        eventId,
                        inventoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalQuantity": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(200));

        verify(inventoryService).update(eq("organizer@example.com"), eq(eventId), eq(inventoryId), any());
    }
}

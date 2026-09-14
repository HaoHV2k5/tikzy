package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.response.TicketTypeResponse;
import com.tikzy.event.service.TicketTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketTypeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class TicketTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketTypeService ticketTypeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_asOrganizer_returnsTicketType() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeResponse response = TicketTypeResponse.builder()
                .id(ticketTypeId)
                .eventId(eventId)
                .name("VIP")
                .price(new BigDecimal("1500000.00"))
                .maxPerOrder(4)
                .isActive(true)
                .build();
        when(ticketTypeService.create(eq("organizer@example.com"), eq(eventId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/ticket-types", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "VIP",
                                  "price": 1500000,
                                  "maxPerOrder": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo hạng vé thành công"))
                .andExpect(jsonPath("$.data.id").value(ticketTypeId.toString()))
                .andExpect(jsonPath("$.data.name").value("VIP"))
                .andExpect(jsonPath("$.data.maxPerOrder").value(4))
                .andExpect(jsonPath("$.data.isActive").value(true));

        verify(ticketTypeService).create(eq("organizer@example.com"), eq(eventId), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/ticket-types", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "VIP",
                                  "price": 1500000
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(ticketTypeService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_withoutName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/ticket-types", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 1500000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400))
                .andExpect(jsonPath("$.errors.name").exists());

        verify(ticketTypeService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_negativePrice_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/ticket-types", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "VIP",
                                  "price": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400))
                .andExpect(jsonPath("$.errors.price").exists());

        verify(ticketTypeService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwn_asOrganizer_returnsTicketTypes() throws Exception {
        UUID eventId = UUID.randomUUID();
        TicketTypeResponse response = TicketTypeResponse.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .name("GA")
                .price(new BigDecimal("500000.00"))
                .maxPerOrder(10)
                .isActive(true)
                .build();
        when(ticketTypeService.getOwn(eq("organizer@example.com"), eq(eventId), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/organizer/events/{eventId}/ticket-types", eventId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("GA"));

        verify(ticketTypeService).getOwn(eq("organizer@example.com"), eq(eventId), eq(true), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwnById_asOrganizer_returnsTicketType() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeResponse response = TicketTypeResponse.builder()
                .id(ticketTypeId)
                .eventId(eventId)
                .name("Early Bird")
                .isActive(true)
                .build();
        when(ticketTypeService.getOwnById("organizer@example.com", eventId, ticketTypeId))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/organizer/events/{eventId}/ticket-types/{ticketTypeId}",
                        eventId,
                        ticketTypeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketTypeId.toString()));

        verify(ticketTypeService).getOwnById("organizer@example.com", eventId, ticketTypeId);
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void update_asOrganizer_returnsUpdatedTicketType() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeResponse response = TicketTypeResponse.builder()
                .id(ticketTypeId)
                .eventId(eventId)
                .name("VIP")
                .price(new BigDecimal("1800000.00"))
                .maxPerOrder(2)
                .isActive(false)
                .build();
        when(ticketTypeService.update(eq("organizer@example.com"), eq(eventId), eq(ticketTypeId), any()))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/organizer/events/{eventId}/ticket-types/{ticketTypeId}",
                        eventId,
                        ticketTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 1800000,
                                  "maxPerOrder": 2,
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.maxPerOrder").value(2));

        verify(ticketTypeService).update(eq("organizer@example.com"), eq(eventId), eq(ticketTypeId), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void delete_asOrganizer_returnsDeletedTicketType() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        TicketTypeResponse response = TicketTypeResponse.builder()
                .id(ticketTypeId)
                .eventId(eventId)
                .name("VIP")
                .isActive(true)
                .build();
        when(ticketTypeService.deleteDraft("organizer@example.com", eventId, ticketTypeId))
                .thenReturn(response);

        mockMvc.perform(delete(
                        "/api/v1/organizer/events/{eventId}/ticket-types/{ticketTypeId}",
                        eventId,
                        ticketTypeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa hạng vé thành công"));

        verify(ticketTypeService).deleteDraft("organizer@example.com", eventId, ticketTypeId);
    }
}

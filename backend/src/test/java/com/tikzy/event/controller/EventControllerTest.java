package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.service.EventService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_asOrganizer_returnsDraftEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .refundPolicy(RefundPolicy.NO_REFUND)
                .build();
        when(eventService.create(eq("organizer@example.com"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "title": "Hòa nhạc mùa hè"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo sự kiện thành công"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Hòa nhạc mùa hè"));

        verify(eventService).create(eq("organizer@example.com"), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "title": "Hòa nhạc mùa hè"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(eventService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_withoutTitle_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400))
                .andExpect(jsonPath("$.errors.title").exists());

        verify(eventService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwn_asOrganizer_returnsEvents() throws Exception {
        EventResponse response = EventResponse.builder()
                .id(UUID.randomUUID())
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .build();
        when(eventService.getOwn(eq("organizer@example.com"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/organizer/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Hòa nhạc mùa hè"));

        verify(eventService).getOwn(eq("organizer@example.com"), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void update_asOrganizer_returnsUpdatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Hòa nhạc đêm")
                .status(EventStatus.DRAFT)
                .build();
        when(eventService.update(eq("organizer@example.com"), eq(eventId), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/organizer/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hòa nhạc đêm"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Hòa nhạc đêm"));

        verify(eventService).update(eq("organizer@example.com"), eq(eventId), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void delete_asOrganizer_returnsDeletedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventResponse response = EventResponse.builder()
                .id(eventId)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .build();
        when(eventService.deleteDraft("organizer@example.com", eventId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/organizer/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa sự kiện thành công"));

        verify(eventService).deleteDraft("organizer@example.com", eventId);
    }
}

package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.response.ShowTimeResponse;
import com.tikzy.event.service.ShowTimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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

@WebMvcTest(controllers = ShowTimeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ShowTimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShowTimeService showTimeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_asOrganizer_returnsShowTime() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        ShowTimeResponse response = ShowTimeResponse.builder()
                .id(showTimeId)
                .eventId(eventId)
                .startTime(LocalDateTime.of(2026, 10, 15, 19, 0))
                .endTime(LocalDateTime.of(2026, 10, 15, 22, 0))
                .isActive(true)
                .build();
        when(showTimeService.create(eq("organizer@example.com"), eq(eventId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/show-times", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-10-15T19:00:00",
                                  "endTime": "2026-10-15T22:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo suất diễn thành công"))
                .andExpect(jsonPath("$.data.id").value(showTimeId.toString()))
                .andExpect(jsonPath("$.data.isActive").value(true));

        verify(showTimeService).create(eq("organizer@example.com"), eq(eventId), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/show-times", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-10-15T19:00:00",
                                  "endTime": "2026-10-15T22:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(showTimeService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_withoutStartTime_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/events/{eventId}/show-times", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endTime": "2026-10-15T22:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400))
                .andExpect(jsonPath("$.errors.startTime").exists());

        verify(showTimeService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwn_asOrganizer_returnsShowTimes() throws Exception {
        UUID eventId = UUID.randomUUID();
        ShowTimeResponse response = ShowTimeResponse.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .startTime(LocalDateTime.of(2026, 10, 15, 19, 0))
                .endTime(LocalDateTime.of(2026, 10, 15, 22, 0))
                .isActive(true)
                .build();
        when(showTimeService.getOwn(eq("organizer@example.com"), eq(eventId), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/organizer/events/{eventId}/show-times", eventId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventId").value(eventId.toString()));

        verify(showTimeService).getOwn(eq("organizer@example.com"), eq(eventId), eq(true), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void getOwnById_asOrganizer_returnsShowTime() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        ShowTimeResponse response = ShowTimeResponse.builder()
                .id(showTimeId)
                .eventId(eventId)
                .isActive(true)
                .build();
        when(showTimeService.getOwnById("organizer@example.com", eventId, showTimeId))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/organizer/events/{eventId}/show-times/{showTimeId}",
                        eventId,
                        showTimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(showTimeId.toString()));

        verify(showTimeService).getOwnById("organizer@example.com", eventId, showTimeId);
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void update_asOrganizer_returnsUpdatedShowTime() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        ShowTimeResponse response = ShowTimeResponse.builder()
                .id(showTimeId)
                .eventId(eventId)
                .isActive(false)
                .build();
        when(showTimeService.update(eq("organizer@example.com"), eq(eventId), eq(showTimeId), any()))
                .thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/organizer/events/{eventId}/show-times/{showTimeId}",
                        eventId,
                        showTimeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        verify(showTimeService).update(eq("organizer@example.com"), eq(eventId), eq(showTimeId), any());
    }

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void delete_asOrganizer_returnsDeletedShowTime() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID showTimeId = UUID.randomUUID();
        ShowTimeResponse response = ShowTimeResponse.builder()
                .id(showTimeId)
                .eventId(eventId)
                .isActive(true)
                .build();
        when(showTimeService.deleteDraft("organizer@example.com", eventId, showTimeId))
                .thenReturn(response);

        mockMvc.perform(delete(
                        "/api/v1/organizer/events/{eventId}/show-times/{showTimeId}",
                        eventId,
                        showTimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa suất diễn thành công"));

        verify(showTimeService).deleteDraft("organizer@example.com", eventId, showTimeId);
    }
}

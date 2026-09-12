package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.response.CategoryRequestResponse;
import com.tikzy.event.enums.CategoryRequestStatus;
import com.tikzy.event.service.CategoryRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryRequestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class CategoryRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryRequestService categoryRequestService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(username = "organizer@example.com", roles = "ORGANIZER")
    void create_asOrganizer_returnsPendingRequest() throws Exception {
        CategoryRequestResponse response = CategoryRequestResponse.builder()
                .id(UUID.randomUUID())
                .proposedName("Nghệ thuật")
                .status(CategoryRequestStatus.PENDING)
                .build();
        when(categoryRequestService.create(eq("organizer@example.com"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/category-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "proposedName": "Nghệ thuật",
                                  "description": "Sự kiện triển lãm"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(categoryRequestService).create(eq("organizer@example.com"), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/category-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "proposedName": "Nghệ thuật"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(categoryRequestService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void review_asAdmin_approvesRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        CategoryRequestResponse response = CategoryRequestResponse.builder()
                .id(requestId)
                .status(CategoryRequestStatus.APPROVED)
                .build();
        when(categoryRequestService.review(
                eq(requestId),
                eq("admin@example.com"),
                any())).thenReturn(response);

        mockMvc.perform(patch(
                        "/api/v1/admin/category-requests/{requestId}/review",
                        requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED",
                                  "slug": "nghe-thuat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(categoryRequestService).review(
                eq(requestId),
                eq("admin@example.com"),
                any());
    }
}

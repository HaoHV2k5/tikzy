package com.tikzy.event.controller;

import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.common.config.JwtAuthenticationFilter;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.config.RestAccessDeniedHandler;
import com.tikzy.common.config.RestAuthenticationEntryPoint;
import com.tikzy.common.config.SecurityConfig;
import com.tikzy.event.dto.response.CategoryResponse;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returnsCreatedCategory() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Triển lãm")
                .slug("trien-lam")
                .sortOrder(0)
                .status(CategoryStatus.DRAFT)
                .build();
        when(categoryService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Triển lãm",
                                  "slug": "trien-lam"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo danh mục thành công"))
                .andExpect(jsonPath("$.data.name").value("Triển lãm"))
                .andExpect(jsonPath("$.data.slug").value("trien-lam"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(categoryService).create(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Triển lãm",
                                  "slug": "trien-lam"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(categoryService, never()).create(any());
    }

    @Test
    void getPublished_asAnonymous_returnsCategories() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Triển lãm")
                .slug("trien-lam")
                .sortOrder(0)
                .status(CategoryStatus.PUBLISHED)
                .build();
        when(categoryService.getPublished(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Triển lãm"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));

        verify(categoryService).getPublished(any());
    }

    @Test
    void getPublishedById_asAnonymous_returnsCategory() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryResponse response = CategoryResponse.builder()
                .id(categoryId)
                .name("Triển lãm")
                .slug("trien-lam")
                .sortOrder(0)
                .status(CategoryStatus.PUBLISHED)
                .build();
        when(categoryService.getPublishedById(categoryId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(categoryService).getPublishedById(categoryId);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAll_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));

        verify(categoryService, never()).getAll(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidSlug_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Triển lãm",
                                  "slug": "Triển lãm"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1400))
                .andExpect(jsonPath("$.errors.slug").exists());

        verify(categoryService, never()).create(any());
    }
}

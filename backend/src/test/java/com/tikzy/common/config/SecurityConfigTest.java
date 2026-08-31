package com.tikzy.common.config;

import com.tikzy.auth.service.AccessTokenRevocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthorizationProbeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccessTokenRevocationService accessTokenRevocationService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_cannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/probe"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizer_canAccessOrganizerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/organizer/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("organizer"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_canAccessOwnEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/customer/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("customer"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void everyKnownRole_canAccessUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));
    }

    @Test
    void anonymousUser_cannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1401));
    }
}

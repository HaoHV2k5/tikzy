package com.tikzy.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra endpoint OpenAPI docs render được sau khi nâng springdoc version —
 * ngăn regression NoSuchMethodError giữa springdoc và Spring Framework ví dụ như 2.6.0.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_loadsWithoutSpringdocError() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh']").exists())
                .andExpect(jsonPath("$.info.title").value(containsString("Tikzy")));
    }
}

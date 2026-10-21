package com.example.admin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 测试基类。
 *
 * <p>走真实过滤器链 —— Spring Boot 会把 springSecurityFilterChain 装进 MockMvc，
 * 所以 401/403、@PreAuthorize 都是真跑一遍，不是 mock 掉的。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestBase {

    protected static final String ADMIN = "admin";
    protected static final String VIEWER = "viewer";
    protected static final String PASSWORD = "admin123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    /** 超级管理员 token，拥有 "*:*:*" 通配权限 */
    protected String adminToken() throws Exception {
        return login(ADMIN, PASSWORD);
    }

    /** 只读用户 token，只有各 list 权限，用于验证 403 */
    protected String viewerToken() throws Exception {
        return login(VIEWER, PASSWORD);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    protected JsonNode readJson(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}

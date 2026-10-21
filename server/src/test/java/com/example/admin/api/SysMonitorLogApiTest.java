package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 操作日志 / 登录日志接口测试。
 */
class SysMonitorLogApiTest extends ApiTestBase {

    @Test
    void loginWritesSuccessLoginLog() throws Exception {
        // 一次成功登录应落一条 status=0 的登录日志
        String token = adminToken();
        mockMvc.perform(get("/api/monitor/loginlog/list")
                        .header("Authorization", bearer(token))
                        .param("username", ADMIN)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].username").value(ADMIN))
                .andExpect(jsonPath("$.data.records[0].status").value(0));
    }

    @Test
    void failedLoginWritesFailureLog() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN, "wrong-password")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/monitor/loginlog/list")
                        .header("Authorization", bearer(adminToken()))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void cleanThenOperLogKeepsOnlyCleanRecord() throws Exception {
        String token = adminToken();
        // 清空会先删光，再由切面补记一条 CLEAN 日志
        mockMvc.perform(post("/api/monitor/operlog/clean")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/monitor/operlog/list")
                        .header("Authorization", bearer(token))
                        .param("title", "操作日志"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[*].businessType", hasItem("CLEAN")));
    }

    @Test
    void viewerCannotListOperLog() throws Exception {
        // readonly 用户没有 monitor:operlog:list，应被 @PreAuthorize 拦为 403
        mockMvc.perform(get("/api/monitor/operlog/list")
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

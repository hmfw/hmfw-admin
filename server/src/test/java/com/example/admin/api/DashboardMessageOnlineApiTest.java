package com.example.admin.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardMessageOnlineApiTest extends ApiTestBase {

    @Test
    void dashboardReturnsMetricsAndSevenDayTrend() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.onlineSessions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.loginTrend.length()").value(7))
                .andExpect(jsonPath("$.data.recentMessages").isArray());
    }

    @Test
    void targetedMessageBecomesReadWhenOpened() throws Exception {
        String admin = adminToken();
        String viewer = viewerToken();
        JsonNode before = readJson(mockMvc.perform(get("/api/message/unread-count")
                        .header("Authorization", bearer(viewer)))
                .andExpect(status().isOk()).andReturn());
        long baseline = before.path("data").asLong();

        JsonNode created = readJson(mockMvc.perform(post("/api/system/notice")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"noticeTitle":"定向测试消息","noticeType":1,"noticeContent":"<p>测试正文</p>",
                                 "status":1,"audienceType":"USER","targetIds":[2]}
                                """))
                .andExpect(status().isOk()).andReturn());
        String noticeId = created.path("data").asText();
        try {
            mockMvc.perform(get("/api/message/unread-count")
                            .header("Authorization", bearer(viewer)))
                    .andExpect(jsonPath("$.data").value(baseline + 1));

            mockMvc.perform(get("/api/message/info").param("id", noticeId)
                            .header("Authorization", bearer(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.noticeTitle").value("定向测试消息"))
                    .andExpect(jsonPath("$.data.readStatus").value(1));

            mockMvc.perform(get("/api/message/unread-count")
                            .header("Authorization", bearer(viewer)))
                    .andExpect(jsonPath("$.data").value(baseline));
        } finally {
            mockMvc.perform(post("/api/system/notice/delete")
                    .header("Authorization", bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\":[" + noticeId + "]}"));
        }
    }

    @Test
    void kickedSessionIsRejectedImmediately() throws Exception {
        String viewer = viewerToken();
        String admin = adminToken();
        JsonNode list = readJson(mockMvc.perform(get("/api/monitor/online/list")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn());
        String viewerSessionId = null;
        for (JsonNode session : list.path("data").path("records")) {
            if (VIEWER.equals(session.path("username").asText())) {
                viewerSessionId = session.path("id").asText();
                break;
            }
        }
        if (viewerSessionId == null) throw new AssertionError("未找到 viewer 在线会话");

        mockMvc.perform(post("/api/monitor/online/kick")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + viewerSessionId + "\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(viewer)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void logoutRevokesCurrentSession() throws Exception {
        String token = viewerToken();
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}

package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysNoticeApiTest extends ApiTestBase {

    @Test
    void createPublishQueryUpdateAndDeleteNotice() throws Exception {
        String token = adminToken();
        String created = mockMvc.perform(post("/api/system/notice")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeTitle\":\"系统维护通知\",\"noticeType\":1,"
                                + "\"noticeContent\":\"<p>今晚维护</p>\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").asLong();

        mockMvc.perform(get("/api/system/notice/info")
                        .header("Authorization", bearer(token))
                        .param("id", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeTitle").value("系统维护通知"))
                .andExpect(jsonPath("$.data.creatorName").value("超级管理员"))
                .andExpect(jsonPath("$.data.publishTime").isNotEmpty());

        mockMvc.perform(get("/api/system/notice/list")
                        .header("Authorization", bearer(token))
                        .param("noticeTitle", "系统维护"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/system/notice/update")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"noticeTitle\":\"系统维护公告\","
                                + "\"noticeType\":2,\"noticeContent\":\"<p>维护完成</p>\",\"status\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/system/notice/info")
                        .header("Authorization", bearer(token))
                        .param("id", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeTitle").value("系统维护公告"))
                .andExpect(jsonPath("$.data.publishTime").doesNotExist());

        mockMvc.perform(post("/api/system/notice/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidNoticeIsRejected() throws Exception {
        mockMvc.perform(post("/api/system/notice")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noticeTitle\":\"\",\"noticeType\":3,\"noticeContent\":\"\",\"status\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void viewerCannotListNotices() throws Exception {
        mockMvc.perform(get("/api/system/notice/list")
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

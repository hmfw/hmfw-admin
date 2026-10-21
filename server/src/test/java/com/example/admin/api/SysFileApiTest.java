package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysFileApiTest extends ApiTestBase {

    @Test
    void uploadListDownloadDelete() throws Exception {
        String token = adminToken();

        // 1. 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/system/file/upload")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originalName").value("test.txt"))
                .andExpect(jsonPath("$.data.size").value(11))
                .andReturn();
        String fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. 列表包含上传的文件
        mockMvc.perform(get("/api/system/file/list")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].originalName").value("test.txt"));

        // MIME 大类可按前缀筛选
        mockMvc.perform(get("/api/system/file/list?mimeType=text/")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        // 3. 下载文件
        MvcResult downloadResult = mockMvc.perform(get("/api/system/file/download?id=" + fileId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadResult.getResponse().getContentAsString()).isEqualTo("hello world");

        // 4. 删除文件
        mockMvc.perform(post("/api/system/file/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + fileId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 5. 删除后列表为空
        mockMvc.perform(get("/api/system/file/list")
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void viewerCannotUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());
        mockMvc.perform(multipart("/api/system/file/upload")
                        .file(file)
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void downloadNonexistentFileReturnsError() throws Exception {
        mockMvc.perform(get("/api/system/file/download?id=99999")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }
}

package com.example.admin.api;

import com.alibaba.excel.EasyExcel;
import com.example.admin.module.user.dto.UserImportRow;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysUserApiTest extends ApiTestBase {

    @Test
    void pageReturnsSeedUsers() throws Exception {
        mockMvc.perform(get("/api/system/user/list").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.records[0].username").value(VIEWER))
                .andExpect(jsonPath("$.data.records[0].roleNames", hasItem("只读用户")))
                .andExpect(jsonPath("$.data.records[1].username").value(ADMIN))
                .andExpect(jsonPath("$.data.records[1].roleNames", hasItem("超级管理员")));
    }

    @Test
    void pageSizeIsHonored() throws Exception {
        mockMvc.perform(get("/api/system/user/list?pageNum=1&pageSize=1")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.pages").value(2));
    }

    @Test
    void usernameFilterWorks() throws Exception {
        mockMvc.perform(get("/api/system/user/list?username=view")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value(VIEWER));
    }

    @Test
    void createThenQueryThenDelete() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(post("/api/system/user")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"alice123","nickname":"爱丽丝",
                                 "email":"alice@example.com","status":1,"roleIds":[2]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").asLong();

        mockMvc.perform(get("/api/system/user/info?id=" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.nickname").value("爱丽丝"))
                .andExpect(jsonPath("$.data.roleIds", hasItem("2")));

        mockMvc.perform(post("/api/system/user/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 逻辑删除后列表里查不到
        mockMvc.perform(get("/api/system/user/list").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/api/system/user")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"whatever123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void viewerCanListButCannotCreate() throws Exception {
        String viewer = viewerToken();

        mockMvc.perform(get("/api/system/user/list").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/system/user")
                        .header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"bob12345\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void builtinAdminCannotBeDeleted() throws Exception {
        mockMvc.perform(post("/api/system/user/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void builtinAdminRolesCannotBeChanged() throws Exception {
        mockMvc.perform(post("/api/system/user/update")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"roleIds\":[2]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void outOfRangePageSizeIsRejected() throws Exception {
        mockMvc.perform(get("/api/system/user/list?pageSize=9999")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void passwordHashIsNeverExposed() throws Exception {
        String body = mockMvc.perform(get("/api/system/user/list")
                        .header("Authorization", bearer(adminToken())))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"password\"");
        assertThat(body).doesNotContain("$2");
    }

    @Test
    void importUsersFromExcel() throws Exception {
        String token = adminToken();

        // 构造测试 Excel：两行有效数据
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UserImportRow row1 = new UserImportRow();
        row1.setUsername("import_alice");
        row1.setNickname("导入爱丽丝");
        row1.setStatus("正常");
        UserImportRow row2 = new UserImportRow();
        row2.setUsername("import_bob");
        row2.setNickname("导入鲍勃");
        row2.setDeptName("研发部");
        row2.setRoleName("只读用户");
        EasyExcel.write(out, UserImportRow.class).sheet("test").doWrite(List.of(row1, row2));

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, out.toByteArray());

        // 导入
        mockMvc.perform(multipart("/api/system/user/import")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failCount").value(0));

        // 验证导入的用户在列表中
        mockMvc.perform(get("/api/system/user/list?username=import_alice")
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].nickname").value("导入爱丽丝"));

        // 清理：删除导入的用户（避免影响后续测试）
        MvcResult listResult = mockMvc.perform(get("/api/system/user/list?username=import_")
                        .header("Authorization", bearer(token)))
                .andReturn();
        var node = objectMapper.readTree(listResult.getResponse().getContentAsString());
        for (var record : node.path("data").path("records")) {
            long id = record.path("id").asLong();
            mockMvc.perform(post("/api/system/user/delete")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[" + id + "]}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void importUsersWithDuplicateReturnsError() throws Exception {
        // 构造 Excel：用户名与已有用户重复
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UserImportRow row = new UserImportRow();
        row.setUsername("admin"); // 与种子数据重复
        EasyExcel.write(out, UserImportRow.class).sheet("test").doWrite(List.of(row));

        MockMultipartFile file = new MockMultipartFile(
                "file", "duplicate.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, out.toByteArray());

        mockMvc.perform(multipart("/api/system/user/import")
                        .file(file)
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failCount").value(1))
                .andExpect(jsonPath("$.data.errors[0].row").value(2))
                .andExpect(jsonPath("$.data.errors[0].username").value("admin"))
                .andExpect(jsonPath("$.data.errors[0].message").value("用户名已存在"));
    }

    @Test
    void exportUsersReturnsExcel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system/user/export")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        // xlsx 是 zip 格式，第一个字节是 PK 签名
        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    void importTemplateReturnsExcel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system/user/importTemplate")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    void viewerCannotImportOrExport() throws Exception {
        // viewer 没有 import/export 权限
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UserImportRow row = new UserImportRow();
        row.setUsername("viewer_import_test");
        EasyExcel.write(out, UserImportRow.class).sheet("test").doWrite(List.of(row));
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, out.toByteArray());

        mockMvc.perform(multipart("/api/system/user/import")
                        .file(file)
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/system/user/export")
                        .header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

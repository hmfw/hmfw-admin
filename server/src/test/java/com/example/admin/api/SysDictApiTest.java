package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字典管理（类型 + 数据）测试。
 */
class SysDictApiTest extends ApiTestBase {

    @Test
    void dictTypeListContainsSeeded() throws Exception {
        mockMvc.perform(get("/api/system/dict/type/list")
                        .header("Authorization", bearer(adminToken()))
                        .param("dictType", "sys_user_sex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].dictName").value("用户性别"));
    }

    @Test
    void listByTypeReturnsEnabledItems() throws Exception {
        mockMvc.perform(get("/api/system/dict/data/type/sys_user_sex")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].dictLabel").value("男"))
                .andExpect(jsonPath("$.data[0].dictValue").value("0"));
    }

    @Test
    void createUpdateDeleteDictData() throws Exception {
        String token = adminToken();
        String created = mockMvc.perform(post("/api/system/dict/data")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictType\":\"sys_user_sex\",\"dictLabel\":\"保密\",\"dictValue\":\"9\",\"sort\":9}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").asLong();

        mockMvc.perform(post("/api/system/dict/data/update")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"dictType\":\"sys_user_sex\",\"dictLabel\":\"保密-改\",\"dictValue\":\"9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/system/dict/data/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void duplicateDictTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/system/dict/type")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictName\":\"重复\",\"dictType\":\"sys_user_sex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void saveCreatesTypeWithItemsAtomically() throws Exception {
        String token = adminToken();
        String body = "{\"dictName\":\"审批状态\",\"dictType\":\"biz_approve_status\",\"status\":1,"
                + "\"dataList\":[{\"dictLabel\":\"待审\",\"dictValue\":\"0\",\"sort\":1},"
                + "{\"dictLabel\":\"通过\",\"dictValue\":\"1\",\"sort\":2}]}";
        mockMvc.perform(post("/api/system/dict/type/save")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 字典项随类型一并落库
        mockMvc.perform(get("/api/system/dict/data/type/biz_approve_status")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dictLabel").value("待审"));

        // 还原：删除新建类型（连带字典项）
        String id = objectMapper.readTree(mockMvc.perform(get("/api/system/dict/type/list")
                        .header("Authorization", bearer(token))
                        .param("dictType", "biz_approve_status"))
                .andReturn().getResponse().getContentAsString())
                .path("data").path("records").get(0).path("id").asText();
        mockMvc.perform(post("/api/system/dict/type/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void saveReconcilesItemsAddUpdateRemove() throws Exception {
        String token = adminToken();
        // 建一个带两项的类型
        mockMvc.perform(post("/api/system/dict/type/save")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictName\":\"渠道\",\"dictType\":\"biz_channel\",\"status\":1,"
                                + "\"dataList\":[{\"dictLabel\":\"线上\",\"dictValue\":\"1\",\"sort\":1},"
                                + "{\"dictLabel\":\"线下\",\"dictValue\":\"2\",\"sort\":2}]}"))
                .andExpect(status().isOk());

        String typeId = objectMapper.readTree(mockMvc.perform(get("/api/system/dict/type/list")
                        .header("Authorization", bearer(token))
                        .param("dictType", "biz_channel"))
                .andReturn().getResponse().getContentAsString())
                .path("data").path("records").get(0).path("id").asText();
        var items = objectMapper.readTree(mockMvc.perform(get("/api/system/dict/data/list")
                        .header("Authorization", bearer(token))
                        .param("dictType", "biz_channel").param("pageSize", "100"))
                .andReturn().getResponse().getContentAsString())
                .path("data").path("records");
        String keepId = items.get(0).path("id").asText(); // 线上，保留并改名

        // 保留并改第一项、删除第二项、新增一项
        String body = "{\"id\":" + typeId + ",\"dictName\":\"渠道\",\"dictType\":\"biz_channel\",\"status\":1,"
                + "\"dataList\":[{\"id\":" + keepId + ",\"dictLabel\":\"线上-改\",\"dictValue\":\"1\",\"sort\":1},"
                + "{\"dictLabel\":\"合作\",\"dictValue\":\"3\",\"sort\":3}]}";
        mockMvc.perform(post("/api/system/dict/type/save")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/system/dict/data/type/biz_channel")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dictLabel").value("线上-改"))
                .andExpect(jsonPath("$.data[1].dictLabel").value("合作"));

        // 还原
        mockMvc.perform(post("/api/system/dict/type/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + typeId + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCannotListDictType() throws Exception {
        mockMvc.perform(get("/api/system/dict/type/list").header("Authorization", bearer(viewerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

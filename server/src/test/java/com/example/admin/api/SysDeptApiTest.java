package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门管理 + 数据范围测试。
 */
class SysDeptApiTest extends ApiTestBase {

    @Test
    void deptTreeReturnsSeededTree() throws Exception {
        mockMvc.perform(get("/api/system/dept/tree").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 单根「总公司」，下挂研发部/运维部
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].deptName").value("总公司"))
                .andExpect(jsonPath("$.data[0].children.length()").value(2));
    }

    @Test
    void createUpdateDeleteDept() throws Exception {
        String token = adminToken();
        String created = mockMvc.perform(post("/api/system/dept")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":1,\"deptName\":\"测试部\",\"sort\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long deptId = objectMapper.readTree(created).path("data").asLong();

        mockMvc.perform(post("/api/system/dept/update")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + deptId + ",\"parentId\":1,\"deptName\":\"测试部-改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/system/dept/info?id=" + deptId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.deptName").value("测试部-改"))
                .andExpect(jsonPath("$.data.ancestors").value("0,1"));

        mockMvc.perform(post("/api/system/dept/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + deptId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deptWithChildrenCannotBeDeleted() throws Exception {
        mockMvc.perform(post("/api/system/dept/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deptWithUsersCannotBeDeleted() throws Exception {
        // 研发部(2) 下挂着 viewer
        mockMvc.perform(post("/api/system/dept/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userListViewerCanLoadDeptTreeForFiltering() throws Exception {
        mockMvc.perform(get("/api/system/dept/tree").header("Authorization", bearer(viewerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].deptName").value("总公司"));
    }

    /**
     * 数据范围端到端：建一个"本部门"范围的角色，挂到研发部(2)的新用户上，
     * 该用户查用户列表时只应看到本部门(dept_id=2)的人，看不到总公司的 admin。
     */
    @Test
    void deptScopeLimitsUserListToOwnDept() throws Exception {
        String admin = adminToken();

        // 1) 建角色：dataScope=3(本部门)，授 system:user:list(菜单 2)
        String roleResp = mockMvc.perform(post("/api/system/role")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"本部门范围\",\"roleKey\":\"dept_scope\",\"dataScope\":3,\"menuIds\":[2]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long roleId = objectMapper.readTree(roleResp).path("data").asLong();

        // 2) 建研发部(2)用户，挂该角色
        String userResp = mockMvc.perform(post("/api/system/user")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"scopetester\",\"password\":\"test123\",\"deptId\":2,\"roleIds\":[" + roleId + "]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(userResp).path("data").asLong();

        // 3) 该用户登录后查用户列表 —— 只应看到研发部(2)的人
        String token = login("scopetester", "test123");
        mockMvc.perform(get("/api/system/user/list").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[*].deptId", everyItem(is("2"))));

        // 4) 清理
        mockMvc.perform(post("/api/system/user/delete")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[" + userId + "]}"));
        mockMvc.perform(post("/api/system/role/delete")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[" + roleId + "]}"));
    }
}

package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysRoleMenuApiTest extends ApiTestBase {

    @Test
    void menuTreeIsNested() throws Exception {
        mockMvc.perform(get("/api/system/menu/tree").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 两个根：系统管理、系统监控
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].menuName").value("系统管理"))
                // 系统管理下面挂 用户/角色/菜单/部门/字典/文件/通知公告 七个菜单
                .andExpect(jsonPath("$.data[0].children.length()").value(7))
                // 用户管理下面挂 7 个按钮权限（查询/新增/修改/删除/重置密码/导入/导出）
                .andExpect(jsonPath("$.data[0].children[0].children.length()").value(7));
    }

    @Test
    void menuTreeCanBeFilteredByName() throws Exception {
        mockMvc.perform(get("/api/system/menu/tree?menuName=角色")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                // 父节点被过滤掉，子节点仍以根节点形式返回，不会凭空消失
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].menuName").value("角色管理"));
    }

    @Test
    void roleDetailContainsMenuIds() throws Exception {
        mockMvc.perform(get("/api/system/role/info?id=1").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleKey").value("admin"))
                .andExpect(jsonPath("$.data.menuIds.length()").value(47));
    }

    @Test
    void assignMenusReplacesPermissions() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(post("/api/system/role")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"临时角色\",\"roleKey\":\"tester\",\"menuIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long roleId = objectMapper.readTree(created).path("data").asLong();

        mockMvc.perform(post("/api/system/role/assignMenus")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + roleId + ",\"menuIds\":[1,2,8,13]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/system/role/info?id=" + roleId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.menuIds.length()").value(4));

        mockMvc.perform(post("/api/system/role/delete")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + roleId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void duplicateRoleKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/system/role")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"重复角色\",\"roleKey\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void roleAssignedToUsersCannotBeDeleted() throws Exception {
        // readonly(2) 已分配给 viewer
        mockMvc.perform(post("/api/system/role/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1007));
    }

    @Test
    void builtinAdminRoleCannotBeDeleted() throws Exception {
        mockMvc.perform(post("/api/system/role/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void menuWithChildrenCannotBeDeleted() throws Exception {
        mockMvc.perform(post("/api/system/menu/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void menuReferencedByRoleCannotBeDeleted() throws Exception {
        // 菜单 3 是「用户查询」按钮，被 admin 角色引用，且没有子节点
        mockMvc.perform(post("/api/system/menu/delete")
                        .header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1008));
    }

    /**
     * 验证权限判断是细粒度的：readonly 被授了「角色管理」菜单（system:role:list），
     * 但没被授「角色查询」按钮（system:role:query）—— 于是能列表、不能看详情。
     */
    @Test
    void viewerCanListRolesButNotQueryOne() throws Exception {
        String viewer = viewerToken();

        mockMvc.perform(get("/api/system/role/list").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/system/role/info?id=1").header("Authorization", bearer(viewer)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void authMenusReturnsOnlyGrantedMenusForViewer() throws Exception {
        // viewer 只被分了 1(目录) / 2 / 8 / 13 四个菜单，且按钮不返回
        mockMvc.perform(get("/api/auth/menus").header("Authorization", bearer(viewerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].menuName").value("系统管理"))
                .andExpect(jsonPath("$.data[0].children.length()").value(3));
    }

    @Test
    void authMenusReturnsFullTreeForAdmin() throws Exception {
        mockMvc.perform(get("/api/auth/menus").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                // 两个根：系统管理、系统监控
                .andExpect(jsonPath("$.data.length()").value(2))
                // 系统管理下面挂 用户/角色/菜单/部门/字典/文件/通知公告 七个菜单
                .andExpect(jsonPath("$.data[0].children.length()").value(7));
    }
}

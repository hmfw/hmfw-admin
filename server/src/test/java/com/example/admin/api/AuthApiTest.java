package com.example.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestBase {

    @Test
    void captchaEndpointIsPublicAndDisabledInTest() throws Exception {
        mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.userInfo.username").value(ADMIN))
                // 超级管理员拿到通配权限
                .andExpect(jsonPath("$.data.userInfo.permissions", hasItem("*:*:*")))
                .andExpect(jsonPath("$.data.userInfo.roles", hasItem("admin")));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void loginWithUnknownUserFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void loginWithoutUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void meReturnsCurrentUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(ADMIN))
                .andExpect(jsonPath("$.data.nickname").value("超级管理员"))
                .andExpect(jsonPath("$.data.permissions", hasItem("*:*:*")));
    }

    @Test
    void refreshReturnsNewTokenForLoggedInUser() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.userInfo.username").value(ADMIN));
    }

    @Test
    void refreshWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void viewerHasOnlyReadPermissions() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(viewerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("readonly")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("*:*:*"))))
                .andExpect(jsonPath("$.data.permissions", hasItem("system:user:list")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("system:user:add"))));
    }

    @Test
    void updateProfileChangesOwnNickname() throws Exception {
        String token = viewerToken();
        try {
            mockMvc.perform(post("/api/auth/profile")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"只读用户-改\",\"email\":\"v2@example.com\",\"phone\":\"13900000000\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                    .andExpect(jsonPath("$.data.nickname").value("只读用户-改"));
        } finally {
            // 还原种子值，避免污染其它用例（MockMvc 测试不回滚）
            mockMvc.perform(post("/api/auth/profile")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nickname\":\"只读用户\",\"email\":\"viewer@example.com\",\"phone\":\"13800000002\"}"));
        }
    }

    @Test
    void updatePasswordWithWrongOldFails() throws Exception {
        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(viewerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong-pwd\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1009));
    }

    @Test
    void changePasswordThenLoginWithNewPassword() throws Exception {
        // 改密码
        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(viewerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"admin123\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 用新密码能登录
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VIEWER, "newpass123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));

        // 还原密码，避免污染其它用例
        String newToken = login(VIEWER, "newpass123");
        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"newpass123\",\"newPassword\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

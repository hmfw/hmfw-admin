package com.example.admin.module.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    /** token 有效期，单位秒 */
    private long expiresIn;
    private UserInfoVO userInfo;

    public static LoginResponse of(String token, long expiresIn, UserInfoVO userInfo) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(expiresIn);
        response.setUserInfo(userInfo);
        return response;
    }
}

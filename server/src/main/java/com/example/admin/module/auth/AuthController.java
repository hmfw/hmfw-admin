package com.example.admin.module.auth;

import com.example.admin.common.api.R;
import com.example.admin.common.ratelimit.RateLimit;
import com.example.admin.module.auth.captcha.CaptchaService;
import com.example.admin.module.auth.dto.CaptchaVO;
import com.example.admin.module.auth.dto.LoginRequest;
import com.example.admin.module.auth.dto.LoginResponse;
import com.example.admin.module.auth.dto.ProfileVO;
import com.example.admin.module.auth.dto.UpdatePasswordRequest;
import com.example.admin.module.auth.dto.UpdateProfileRequest;
import com.example.admin.module.auth.dto.UserInfoVO;
import com.example.admin.module.menu.dto.SysMenuTreeVO;
import com.example.admin.module.menu.service.SysMenuService;
import com.example.admin.module.user.service.SysUserService;
import com.example.admin.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "认证", description = "登录、登出、当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysMenuService sysMenuService;
    private final SysUserService sysUserService;
    private final CaptchaService captchaService;

    @Operation(summary = "图形验证码", description = "返回 base64 图片与 captchaKey；enabled=false 时前端跳过验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "登录", description = "成功返回 JWT，后续请求带 Authorization: Bearer <token>")
    @RateLimit(count = 10, seconds = 60)
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return R.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "登出", description = "注销当前 JWT 会话，使令牌立即失效")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout(SecurityUtils.getLoginUser());
        return R.ok();
    }

    @Operation(summary = "刷新 token", description = "凭当前有效 token 换新 token，用于滑动续期")
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(HttpServletRequest request) {
        return R.ok(authService.refresh(SecurityUtils.getLoginUser(), request));
    }

    @Operation(summary = "当前登录用户信息", description = "含角色与权限集合")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(authService.toUserInfo(SecurityUtils.getLoginUser()));
    }

    @Operation(summary = "当前用户可见的菜单树", description = "供前端生成动态路由，超级管理员返回全量")
    @GetMapping("/menus")
    public R<List<SysMenuTreeVO>> menus() {
        return R.ok(sysMenuService.menusOfCurrentUser());
    }

    @Operation(summary = "修改个人资料", description = "当前登录用户改自己的昵称/邮箱/手机号")
    @PostMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        sysUserService.updateProfile(SecurityUtils.getUserId(),
                request.getNickname(), request.getEmail(), request.getPhone());
        return R.ok();
    }

    @Operation(summary = "个人资料", description = "当前登录用户的基础资料，含邮箱/手机号")
    @GetMapping("/profile")
    public R<ProfileVO> profile() {
        return R.ok(authService.getProfile(SecurityUtils.getUserId()));
    }

    @Operation(summary = "修改密码", description = "当前登录用户改自己的密码，需校验原密码")
    @PostMapping("/password")
    public R<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        sysUserService.changePassword(SecurityUtils.getUserId(),
                request.getOldPassword(), request.getNewPassword());
        return R.ok();
    }
}

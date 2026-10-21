package com.example.admin.module.auth;

import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.auth.captcha.CaptchaService;
import com.example.admin.module.auth.dto.LoginRequest;
import com.example.admin.module.auth.dto.LoginResponse;
import com.example.admin.module.auth.dto.ProfileVO;
import com.example.admin.module.auth.dto.UserInfoVO;
import com.example.admin.module.monitor.loginlog.service.SysLoginLogService;
import com.example.admin.module.monitor.online.service.SysOnlineSessionService;
import com.example.admin.module.user.dto.SysUserVO;
import com.example.admin.module.user.service.SysUserService;
import com.example.admin.security.JwtUtils;
import com.example.admin.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserService sysUserService;
    private final SysLoginLogService sysLoginLogService;
    private final CaptchaService captchaService;
    private final SysOnlineSessionService onlineSessionService;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 先校验验证码（开关关闭时 verify 恒为 true）
        if (!captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode())) {
            sysLoginLogService.record(request.getUsername(), clientIp(), false, ResultCode.CAPTCHA_ERROR.getMsg());
            throw new BizException(ResultCode.CAPTCHA_ERROR);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (DisabledException e) {
            sysLoginLogService.record(request.getUsername(), clientIp(), false, ResultCode.ACCOUNT_DISABLED.getMsg());
            throw BizException.unauthorized(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            // 用户名不存在与密码错误在 Spring Security 里被统一成 BadCredentialsException，
            // 对外也统一提示，避免泄露账号是否存在
            sysLoginLogService.record(request.getUsername(), clientIp(), false, ResultCode.LOGIN_FAILED.getMsg());
            throw BizException.unauthorized(ResultCode.LOGIN_FAILED);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtUtils.createToken(loginUser.getUserId(), loginUser.getUsername());
        onlineSessionService.register(token, loginUser, httpRequest);
        sysLoginLogService.record(loginUser.getUsername(), clientIp(), true, "登录成功");
        return LoginResponse.of(token, jwtUtils.getExpireSeconds(), toUserInfo(loginUser));
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
                int comma = xff.indexOf(',');
                return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    public UserInfoVO toUserInfo(LoginUser loginUser) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        return vo;
    }

    /** 用当前有效登录态换一个新 token（滑动续期），避免活跃用户被动过期 */
    public LoginResponse refresh(LoginUser loginUser, HttpServletRequest request) {
        String token = jwtUtils.createToken(loginUser.getUserId(), loginUser.getUsername());
        onlineSessionService.register(token, loginUser, request);
        onlineSessionService.revoke(loginUser.getSessionId());
        return LoginResponse.of(token, jwtUtils.getExpireSeconds(), toUserInfo(loginUser));
    }

    public void logout(LoginUser loginUser) {
        onlineSessionService.revoke(loginUser.getSessionId());
    }

    /** 个人中心：当前用户的基础资料（含邮箱/手机号） */
    public ProfileVO getProfile(Long userId) {
        SysUserVO user = sysUserService.getDetail(userId);
        ProfileVO vo = new ProfileVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        return vo;
    }
}

package com.example.admin.module.monitor.online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.module.monitor.online.dto.SysOnlineSessionQuery;
import com.example.admin.module.monitor.online.entity.SysOnlineSession;
import com.example.admin.module.monitor.online.mapper.SysOnlineSessionMapper;
import com.example.admin.security.JwtUtils;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysOnlineSessionService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SysOnlineSessionMapper mapper;
    private final JwtUtils jwtUtils;

    public void register(String token, LoginUser user, HttpServletRequest request) {
        Claims claims = jwtUtils.parse(token);
        LocalDateTime now = LocalDateTime.now(ZONE);
        SysOnlineSession session = new SysOnlineSession();
        session.setId(claims.getId());
        session.setUserId(user.getUserId());
        session.setUsername(user.getUsername());
        session.setNickname(user.getNickname());
        session.setIp(clientIp(request));
        session.setUserAgent(trimTo(request.getHeader("User-Agent"), 500));
        session.setLoginTime(now);
        session.setLastActiveTime(now);
        session.setExpiresTime(toLocalDateTime(claims.getExpiration()));
        session.setStatus(1);
        mapper.insert(session);
    }

    public boolean activate(String sessionId, LoginUser user) {
        SysOnlineSession session = mapper.selectById(sessionId);
        if (session == null || session.getStatus() != 1
                || session.getExpiresTime().isBefore(LocalDateTime.now(ZONE))) {
            return false;
        }
        user.setSessionId(sessionId);
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (session.getLastActiveTime().isBefore(now.minusMinutes(1))) {
            session.setLastActiveTime(now);
            mapper.updateById(session);
        }
        return true;
    }

    public void revoke(String sessionId) {
        if (!StringUtils.hasText(sessionId)) return;
        SysOnlineSession session = mapper.selectById(sessionId);
        if (session != null && session.getStatus() == 1) {
            session.setStatus(0);
            mapper.updateById(session);
        }
    }

    public void kick(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        mapper.update(null, Wrappers.<SysOnlineSession>lambdaUpdate()
                .in(SysOnlineSession::getId, ids)
                .set(SysOnlineSession::getStatus, 0));
    }

    public PageResult<SysOnlineSession> page(SysOnlineSessionQuery query) {
        expireSessions();
        Page<SysOnlineSession> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOnlineSession> wrapper = Wrappers.<SysOnlineSession>lambdaQuery()
                .eq(SysOnlineSession::getStatus, 1)
                .gt(SysOnlineSession::getExpiresTime, LocalDateTime.now(ZONE))
                .like(StringUtils.hasText(query.getUsername()), SysOnlineSession::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getIp()), SysOnlineSession::getIp, query.getIp())
                .orderByDesc(SysOnlineSession::getLastActiveTime);
        mapper.selectPage(page, wrapper);
        String currentId = SecurityUtils.getLoginUser().getSessionId();
        page.getRecords().forEach(item -> item.setCurrent(item.getId().equals(currentId)));
        return PageResult.of(page);
    }

    public long countOnlineUsers() {
        expireSessions();
        return mapper.selectCount(Wrappers.<SysOnlineSession>lambdaQuery()
                .eq(SysOnlineSession::getStatus, 1)
                .gt(SysOnlineSession::getExpiresTime, LocalDateTime.now(ZONE))
                .select(SysOnlineSession::getUserId));
    }

    private void expireSessions() {
        mapper.update(null, Wrappers.<SysOnlineSession>lambdaUpdate()
                .eq(SysOnlineSession::getStatus, 1)
                .le(SysOnlineSession::getExpiresTime, LocalDateTime.now(ZONE))
                .set(SysOnlineSession::getStatus, 0));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZONE);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff) && !"unknown".equalsIgnoreCase(xff)) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}

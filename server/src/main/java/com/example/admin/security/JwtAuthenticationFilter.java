package com.example.admin.security;

import com.example.admin.common.constant.SecurityConstants;
import com.example.admin.module.monitor.online.service.SysOnlineSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final SysOnlineSessionService onlineSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtUtils.parse(token);
                // 每次都重新装载：这样停用用户立即失效，不必等 token 过期
                LoginUser user = (LoginUser) userDetailsService.loadUserByUsername(claims.getSubject());
                if (user.isEnabled() && onlineSessionService.activate(claims.getId(), user)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                // 不在此处写响应，交给 AuthenticationEntryPoint 统一输出；
                // 更不能抛出去 —— 抛了会被当成 403
                SecurityContextHolder.clearContext();
                request.setAttribute(SecurityConstants.AUTH_ERROR_ATTR, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtUtils.getHeader());
        if (header == null) {
            return null;
        }
        String prefix = jwtUtils.getPrefix();
        if (header.length() > prefix.length()
                && header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return header.substring(prefix.length()).trim();
        }
        return null;
    }
}

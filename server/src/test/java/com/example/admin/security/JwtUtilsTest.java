package com.example.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 纯单元测试，不起 Spring 上下文。
 * 这里同时充当 jjwt 0.13 API 写法的验证 —— 若 API 用错，编译期就挂了。
 */
class JwtUtilsTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        jwtUtils.setSecret(SECRET);
        jwtUtils.setIssuer("admin");
        jwtUtils.setExpireMinutes(120);
    }

    @Test
    void createAndParseRoundTrip() {
        String token = jwtUtils.createToken(42L, "alice");

        Claims claims = jwtUtils.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(jwtUtils.getUserId(claims)).isEqualTo(42L);
        assertThat(claims.getIssuer()).isEqualTo("admin");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void expiredTokenIsRejected() {
        // 过期 10 分钟，超出 clockSkewSeconds(60) 的容差
        jwtUtils.setExpireMinutes(-10);
        String token = jwtUtils.createToken(1L, "alice");

        assertThatThrownBy(() -> jwtUtils.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtils.createToken(1L, "alice");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwtUtils.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = jwtUtils.createToken(1L, "alice");

        JwtUtils other = new JwtUtils();
        other.setSecret("another-secret-key-that-is-long-enough-32bytes");
        other.setIssuer("admin");

        assertThatThrownBy(() -> other.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithDifferentIssuerIsRejected() {
        String token = jwtUtils.createToken(1L, "alice");

        JwtUtils other = new JwtUtils();
        other.setSecret(SECRET);
        other.setIssuer("someone-else");

        assertThatThrownBy(() -> other.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRejected() {
        JwtUtils weak = new JwtUtils();
        weak.setSecret("too-short");

        assertThatThrownBy(() -> weak.createToken(1L, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void expireSecondsDerivedFromMinutes() {
        assertThat(jwtUtils.getExpireSeconds()).isEqualTo(7200L);
    }
}

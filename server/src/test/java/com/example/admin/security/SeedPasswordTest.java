package com.example.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止 data.sql 里的 BCrypt 密文写错 —— 这类错误肉眼看不出来，
 * 但会直接导致登录全挂，属于最该被自动化测试盯住的地方。
 */
@SpringBootTest
@ActiveProfiles("test")
class SeedPasswordTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedPasswordIsAdmin123() {
        String hash = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = 'admin'", String.class);

        assertThat(hash).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("admin123", hash)).isTrue();
    }

    @Test
    void viewerPasswordIsAlsoAdmin123() {
        String hash = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = 'viewer'", String.class);

        assertThat(new BCryptPasswordEncoder().matches("admin123", hash)).isTrue();
    }
}

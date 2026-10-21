package com.example.admin.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DDL 哨兵。
 *
 * schema.sql 被测试（H2 MySQL 模式）和生产（MySQL）共用。一旦有人往里面写了
 * H2 不认的语法，这里会立刻变红 —— 而不是等到真机上导库时才炸。
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allTablesExist() {
        assertThat(count("sys_user")).isEqualTo(2L);
        assertThat(count("sys_role")).isEqualTo(2L);
        assertThat(count("sys_menu")).isEqualTo(47L);
        assertThat(count("sys_user_role")).isEqualTo(2L);
        // admin 角色 47 条（全部菜单） + readonly 角色 4 条
        assertThat(count("sys_role_menu")).isEqualTo(51L);
        assertThat(count("sys_dept")).isEqualTo(3L);
        assertThat(count("sys_dict_type")).isEqualTo(5L);
        assertThat(count("sys_dict_data")).isEqualTo(11L);
        assertThat(count("sys_file")).isZero();
        assertThat(count("sys_notice")).isZero();
        assertThat(count("sys_notice_target")).isZero();
        assertThat(count("sys_notice_read")).isZero();
        assertThat(count("sys_online_session")).isZero();
    }

    @Test
    void sysUserColumnsMatchEntity() {
        // 能把这几个列全查出来，说明列名/类型与实体对得上
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin' "
                        + "AND password IS NOT NULL AND status = 1 AND deleted = 0 "
                        + "AND create_time IS NOT NULL AND update_time IS NOT NULL",
                Long.class);
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void logicalDeleteFlagIsUsable() {
        // deleted 是逻辑删除字段，必须能当普通数值列读写
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM sys_user WHERE username = 'admin'", Integer.class);
        assertThat(deleted).isZero();
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table, Long.class);
    }
}

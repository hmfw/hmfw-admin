package com.example.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /**
     * 方言由 admin.mybatis.db-type 配置切换，默认 H2。
     * 注意：PaginationInnerInterceptor 没有无参构造，必须显式传 DbType。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            @Value("${admin.mybatis.db-type:H2}") String dbType) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination =
                new PaginationInnerInterceptor(DbType.valueOf(dbType));
        pagination.setOverflow(false);   // 页码越界返回空列表，而不是回到首页
        pagination.setMaxLimit(500L);    // 单页上限，防止一次拉全表

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}

package com.example.admin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * createTime / updateTime 由应用层填充，不依赖数据库的 ON UPDATE CURRENT_TIMESTAMP
 * —— 那样 H2 跑不了，DDL 就无法在测试与生产间共用。
 * strictInsertFill 只在字段标了 FieldFill.INSERT / INSERT_UPDATE 且当前为 null 时才写入。
 * 显式锚定 Asia/Shanghai：LocalDateTime 不带时区，若服务器 JVM 跑在 UTC（如云主机），
 * LocalDateTime.now() 会落后 8 小时且无从校正，这里固定北京时间消除该隐患。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now(ZONE));
    }
}

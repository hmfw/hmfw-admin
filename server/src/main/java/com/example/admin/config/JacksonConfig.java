package com.example.admin.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.time.format.DateTimeFormatter;

/**
 * 统一 java.time 的 JSON 格式，避免默认的 ISO（带 T、可能带纳秒）直接发给前端。
 * LocalDateTime 本身不含时区，只负责"墙上时间"的格式；真正的时区锚点见 {@link MyMetaObjectHandler}。
 * 序列化 + 反序列化都覆盖，这样请求体里的时间字符串同样按此格式解析。
 *
 * <p>另外把装箱 {@link Long}/{@link BigInteger} 序列化成字符串下发：JS 的 number 安全整数上限只有
 * 2^53-1，雪花 id 等大整数当数字传会丢精度。这里只注册装箱 Long，不注册原始 long，
 * 因此 id（装箱 Long）变字符串，而 {@code PageResult.total}、{@code R.timestamp} 等原始 long 的
 * 计数/时间戳仍保持数字。入站方向无需处理：Jackson 默认会把字符串 "123" 强制转成 Long。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeFormatCustomizer() {
        return builder -> {
            builder.serializers(
                    new LocalDateTimeSerializer(DATE_TIME),
                    new LocalDateSerializer(DATE),
                    new LocalTimeSerializer(TIME));
            builder.deserializers(
                    new LocalDateTimeDeserializer(DATE_TIME),
                    new LocalDateDeserializer(DATE),
                    new LocalTimeDeserializer(TIME));

            // 装箱 Long / BigInteger -> 字符串，规避前端 JS 大整数精度丢失（雪花 id 等）
            SimpleModule bigNumberToString = new SimpleModule();
            bigNumberToString.addSerializer(Long.class, ToStringSerializer.instance);
            bigNumberToString.addSerializer(BigInteger.class, ToStringSerializer.instance);
            builder.modulesToInstall(bigNumberToString);
        };
    }
}

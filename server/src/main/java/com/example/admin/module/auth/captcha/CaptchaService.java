package com.example.admin.module.auth.captcha;

import com.example.admin.module.auth.dto.CaptchaVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码：无状态服务端临时存储。
 *
 * <p>答案存在进程内 {@link ConcurrentHashMap}（带过期时间），一次性消费。
 * 单实例部署足够；多实例/生产应换成 Redis —— 这里刻意不引额外中间件。
 */
@Service
public class CaptchaService {

    /** 去掉易混字符（0/O/1/I/l） */
    private static final char[] POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Value("${admin.captcha.enabled:true}")
    private boolean enabled;

    @Value("${admin.captcha.expire-seconds:120}")
    private long expireSeconds;

    public boolean isEnabled() {
        return enabled;
    }

    /** 生成一张验证码；关闭时返回 enabled=false 的空壳 */
    public CaptchaVO generate() {
        if (!enabled) {
            CaptchaVO vo = new CaptchaVO();
            vo.setEnabled(false);
            return vo;
        }
        evictExpired();

        String code = randomCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        store.put(key, new Entry(code, System.currentTimeMillis() + expireSeconds * 1000));

        CaptchaVO vo = new CaptchaVO();
        vo.setEnabled(true);
        vo.setCaptchaKey(key);
        vo.setCaptchaImage(toBase64Image(code));
        return vo;
    }

    /**
     * 校验并消费验证码（无论成功与否都移除，防重放）。
     * 验证码关闭时恒为 true。
     */
    public boolean verify(String key, String code) {
        if (!enabled) {
            return true;
        }
        if (key == null || code == null) {
            return false;
        }
        Entry entry = store.remove(key);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()) {
            return false;
        }
        return entry.code().equalsIgnoreCase(code);
    }

    private String randomCode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(POOL[random.nextInt(POOL.length)]);
        }
        return sb.toString();
    }

    private String toBase64Image(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Arial", Font.BOLD, 28));

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(random, 160, 220));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        // 字符
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(random, 20, 130));
            int x = 15 + i * 26 + random.nextInt(6);
            int y = 28 + random.nextInt(6);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    private Color randomColor(ThreadLocalRandom random, int min, int max) {
        return new Color(
                min + random.nextInt(max - min),
                min + random.nextInt(max - min),
                min + random.nextInt(max - min));
    }

    /** 简单的过期清理：生成时顺带清一遍，避免 map 无限增长 */
    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt() < now);
    }

    private record Entry(String code, long expireAt) {
    }
}

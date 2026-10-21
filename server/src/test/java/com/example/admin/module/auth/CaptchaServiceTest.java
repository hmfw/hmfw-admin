package com.example.admin.module.auth;

import com.example.admin.module.auth.captcha.CaptchaService;
import com.example.admin.module.auth.dto.CaptchaVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码核心逻辑单测：不起 Web 上下文，直接操作 {@link CaptchaService}。
 */
class CaptchaServiceTest {

    private CaptchaService newService(boolean enabled) {
        CaptchaService service = new CaptchaService();
        ReflectionTestUtils.setField(service, "enabled", enabled);
        ReflectionTestUtils.setField(service, "expireSeconds", 120L);
        return service;
    }

    @Test
    void disabledServiceSkipsVerification() {
        CaptchaService service = newService(false);
        CaptchaVO vo = service.generate();
        assertThat(vo.isEnabled()).isFalse();
        assertThat(vo.getCaptchaKey()).isNull();
        // 关闭时任意输入都放行
        assertThat(service.verify(null, null)).isTrue();
        assertThat(service.verify("x", "y")).isTrue();
    }

    @Test
    void generateProducesKeyAndImage() {
        CaptchaService service = newService(true);
        CaptchaVO vo = service.generate();
        assertThat(vo.isEnabled()).isTrue();
        assertThat(vo.getCaptchaKey()).isNotBlank();
        assertThat(vo.getCaptchaImage()).startsWith("data:image/png;base64,");
    }

    @Test
    void correctCodeVerifiesOnceThenConsumed() throws Exception {
        CaptchaService service = newService(true);
        CaptchaVO vo = service.generate();
        String code = peekCode(service, vo.getCaptchaKey());

        // 大小写不敏感
        assertThat(service.verify(vo.getCaptchaKey(), code.toLowerCase())).isTrue();
        // 一次性消费，第二次失败
        assertThat(service.verify(vo.getCaptchaKey(), code)).isFalse();
    }

    @Test
    void wrongCodeOrKeyFails() {
        CaptchaService service = newService(true);
        CaptchaVO vo = service.generate();
        assertThat(service.verify(vo.getCaptchaKey(), "----")).isFalse();
        assertThat(service.verify("nonexistent-key", "AAAA")).isFalse();
    }

    @SuppressWarnings("unchecked")
    private String peekCode(CaptchaService service, String key) throws Exception {
        Map<String, Object> store = (Map<String, Object>) ReflectionTestUtils.getField(service, "store");
        Object entry = store.get(key);
        Field f = entry.getClass().getDeclaredField("code");
        f.setAccessible(true);
        return (String) f.get(entry);
    }
}

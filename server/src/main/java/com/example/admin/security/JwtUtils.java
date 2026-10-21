package com.example.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 *
 * <p>用的是 jjwt 0.13 的新 API。旧教程里的 {@code parserBuilder()} / {@code setSigningKey()} /
 * {@code parseClaimsJws()} / {@code SignatureAlgorithm} 枚举全部已废弃或移除，抄进来编译不过。
 */
@Component
@ConfigurationProperties(prefix = "admin.jwt")
@Setter
public class JwtUtils {

    public static final String CLAIM_USER_ID = "uid";

    private String secret;
    private long expireMinutes = 120;
    private String issuer = "admin";
    private String header = "Authorization";
    /** 不带尾随空格，解析时自行 trim */
    private String prefix = "Bearer";

    private volatile SecretKey cachedKey;

    public String getHeader() {
        return header;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getExpireSeconds() {
        return expireMinutes * 60L;
    }

    private SecretKey key() {
        SecretKey k = cachedKey;
        if (k == null) {
            synchronized (this) {
                k = cachedKey;
                if (k == null) {
                    byte[] bytes = secret == null
                            ? new byte[0]
                            : secret.getBytes(StandardCharsets.UTF_8);
                    // jjwt 强制 HMAC 密钥 >= 256 bit，短了抛 WeakKeyException
                    if (bytes.length < 32) {
                        throw new IllegalStateException("admin.jwt.secret 至少需要 32 个字符");
                    }
                    k = Keys.hmacShaKeyFor(bytes);
                    cachedKey = k;
                }
            }
        }
        return k;
    }

    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并校验签名、有效期、issuer。
     * 失败时抛 {@link io.jsonwebtoken.JwtException} 的子类，捕获这一个基类即可全覆盖。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(issuer)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        Number n = claims.get(CLAIM_USER_ID, Number.class);
        return n == null ? null : n.longValue();
    }
}

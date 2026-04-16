package com.xixi.security;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.xixi.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtProperties jwtProperties;
    public String generateToken(LoginUser loginUser){
        Date now = new Date();
        Date exprireAt = DateUtil.offsetMinute(now,jwtProperties.getExpireMinutes().intValue());
        String jti = UUID.randomUUID().toString();
        Map<String,Object> payload =new HashMap<>();
        payload.put("uid",loginUser.getUserId());
        payload.put("sub",loginUser.getUsername());
        payload.put("exp", exprireAt);
        payload.put("jti",jti);
        payload.put("roles",loginUser.getRoleCodes());
        payload.put("iat",now);
        return JWTUtil.createToken(payload,jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
    public JWT parseToken(String token) {
        return JWTUtil.parseToken(token)
                .setKey(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            JWT jwt = parseToken(token);
            return jwt.verify() && jwt.validate(0);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(String token) {
        Object username = parseToken(token).getPayload("sub");
        if (username == null) {
            return null;
        }
        return username.toString();
    }

    public Long getUserId(String token) {
        return getLongPayload(token, "uid");
    }

    public String getJti(String token) {
        Object jti = parseToken(token).getPayload("jti");
        if (jti == null) {
            return null;
        }
        return jti.toString();
    }

    public long getRemainingMillis(String token) {
        Long expSeconds = getLongPayload(token, "exp");
        if (expSeconds == null) {
            return 0L;
        }
        long expireMillis = expSeconds * 1000L;
        long remainingMillis = expireMillis - System.currentTimeMillis();
        return Math.max(remainingMillis, 0L);
    }

    private Long getLongPayload(String token, String payloadKey) {
        Object value = parseToken(token).getPayload(payloadKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}

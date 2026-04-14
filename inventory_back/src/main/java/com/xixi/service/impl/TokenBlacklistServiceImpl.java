package com.xixi.service.impl;

import com.xixi.config.JwtProperties;
import com.xixi.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public void blacklist(String jti, long ttlMillis) {
        String key = jwtProperties.getBlacklistPrefix() + jti;
        stringRedisTemplate.opsForValue().set(key,"1",ttlMillis,TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklist(String jti) {
        String key = jwtProperties.getBlacklistPrefix() + jti;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}

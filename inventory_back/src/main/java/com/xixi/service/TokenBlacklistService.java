package com.xixi.service;

public interface TokenBlacklistService {
    void blacklist(String jti,long ttlMillis);
    boolean isBlacklist(String jti);
}

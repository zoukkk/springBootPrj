package com.example.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class TokenSessionService {
    private static final int MAX_SESSIONS = 2;
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String USER_PREFIX = "auth:user:";

    private final StringRedisTemplate redisTemplate;

    public TokenSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(Long userId, String tokenId, Instant issuedAt, Duration ttl) {
        String sessionsKey = userSessionsKey(userId);
        String sequenceKey = userSequenceKey(userId);
        Long sequence = redisTemplate.opsForValue().increment(sequenceKey);
        redisTemplate.opsForValue().set(tokenKey(tokenId), String.valueOf(userId), ttl);
        redisTemplate.opsForZSet().add(sessionsKey, tokenId,
                sequence == null ? issuedAt.toEpochMilli() : sequence.doubleValue());
        redisTemplate.expire(sessionsKey, ttl.plusMinutes(1));
        redisTemplate.expire(sequenceKey, ttl.plusMinutes(1));

        Long size = redisTemplate.opsForZSet().zCard(sessionsKey);
        if (size != null && size > MAX_SESSIONS) {
            Set<String> oldest = redisTemplate.opsForZSet()
                    .range(sessionsKey, 0, size - MAX_SESSIONS - 1);
            if (oldest != null) {
                oldest.forEach(item -> redisTemplate.delete(tokenKey(item)));
                redisTemplate.opsForZSet().remove(sessionsKey, oldest.toArray());
            }
        }
    }

    public boolean isValid(Long userId, String tokenId) {
        String storedUserId = redisTemplate.opsForValue().get(tokenKey(tokenId));
        return String.valueOf(userId).equals(storedUserId);
    }

    public void revoke(Long userId, String tokenId) {
        redisTemplate.delete(tokenKey(tokenId));
        redisTemplate.opsForZSet().remove(userSessionsKey(userId), tokenId);
    }

    private String tokenKey(String tokenId) {
        return TOKEN_PREFIX + tokenId;
    }

    private String userSessionsKey(Long userId) {
        return USER_PREFIX + userId + ":sessions";
    }

    private String userSequenceKey(Long userId) {
        return USER_PREFIX + userId + ":sequence";
    }
}

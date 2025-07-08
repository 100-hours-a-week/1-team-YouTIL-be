package com.youtil.Api.Github.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.io.SerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubCacheHelper {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시에서 데이터 조회 후 없으면 fallback 실행
    public <T> T getFromCacheWithFallback(String cacheKey, String cacheType, Class<T> clazz, Supplier<T> fallback) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("캐시 hit: {} (type: {})", cacheKey, cacheType);
                return objectMapper.readValue(cached, clazz);
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 데이터 파싱 실패, 삭제 후 재조회 - key: {}, type: {}", cacheKey, cacheType);
            deleteCacheSafely(cacheKey);
        } catch (SerializationException e) {
            log.warn("Redis 직렬화 오류, 캐시 삭제 - key: {}", cacheKey);
            deleteCacheSafely(cacheKey);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패, 직접 API 호출 - type: {}, error: {}", cacheType, e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 캐시 오류 - key: {}, type: {}, error: {}", cacheKey, cacheType, e.getMessage());
        }

        log.debug("캐시 miss, API 호출 - key: {}, type: {}", cacheKey, cacheType);
        return fallback.get();
    }

    // 데이터를 캐시에 저장
    public <T> void saveToCache(String cacheKey, T data, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("캐시 저장 성공 - key: {}, size: {}bytes", cacheKey, json.length());
        } catch (JsonProcessingException e) {
            log.warn("캐시 데이터 직렬화 실패 - key: {}, error: {}", cacheKey, e.getMessage());
        } catch (SerializationException e) {
            log.warn("Redis 직렬화 오류로 캐시 저장 실패 - key: {}, error: {}", cacheKey, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 캐시 저장 실패 - key: {}, error: {}", cacheKey, e.getMessage());
        } catch (Exception e) {
            log.warn("캐시 저장 중 예상치 못한 오류 - key: {}, error: {}", cacheKey, e.getMessage());
        }
    }

    private void deleteCacheSafely(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
            log.debug("캐시 삭제 성공 - key: {}", cacheKey);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패로 캐시 삭제 실패 - key: {}, error: {}", cacheKey, e.getMessage());
        } catch (Exception e) {
            log.error("캐시 삭제 중 예상치 못한 오류 - key: {}, error: {}", cacheKey, e.getMessage());
        }
    }
}

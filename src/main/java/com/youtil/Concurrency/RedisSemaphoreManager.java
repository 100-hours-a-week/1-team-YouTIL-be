package com.youtil.Concurrency;


import com.youtil.Common.Enums.AiType;
import com.youtil.Concurrency.policy.SemaphorePolicy;
import com.youtil.Concurrency.policy.SemaphorePolicySelector;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSemaphoreManager {

    private static final String ACQUIRE_SEMAPHORE_LUA =
            """
                    -- 중복 요청 먼저 체크
                    if redis.call('sismember', KEYS[1], ARGV[3]) == 1 or redis.call('sismember', KEYS[2], ARGV[3]) == 1 then
                        return "duplicated"
                    end
                    
                    -- fixed 자원 확인 및 등록
                    if redis.call('scard', KEYS[1]) < tonumber(ARGV[1]) then
                        redis.call('sadd', KEYS[1], ARGV[3])
                        redis.call('expire', KEYS[1], tonumber(ARGV[4]))
                        return "fixed"
                    end
                    
                    -- shared 자원 확인 및 등록
                    if redis.call('scard', KEYS[2]) < tonumber(ARGV[2]) then
                        redis.call('sadd', KEYS[2], ARGV[3])
                        redis.call('expire', KEYS[2], tonumber(ARGV[4]))
                        return "shared"
                    end
                    
                    -- 자원 모두 소진
                    return "none"
                    """;
    private static final byte[] ACQUIRE_SEMAPHORE_LUA_BYTES = ACQUIRE_SEMAPHORE_LUA.getBytes(
            StandardCharsets.UTF_8);
    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    private static final String SHARED_SEMAPHORE_KEY = "semaphore:shared";
    private static final String SEMAPHORE_KEY_PREFIX = "semaphore:";
    private static final String SEMAPHORE_KEY_SUFFIX = ":fixed";
    private static final Duration SEMAPHORE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final SemaphorePolicySelector semaphorePolicySelector;

    public boolean tryAcquireSemaphore(String requestId, String resourceType) {
        Optional<AiType> optionalAiType = AiType.from(resourceType);
        if (optionalAiType.isEmpty()) {
            log.warn("해당 타입이 존재하지 않습니다.: {}", resourceType);
            return false;
        }

        AiType aiType = optionalAiType.get();
        SemaphorePolicy policy = semaphorePolicySelector.getSemaphorePolicy();

        String fixedKey = getFixedKey(resourceType);
        String sharedKey = SHARED_SEMAPHORE_KEY;

        byte[] fixedKeyBytes = STRING_SERIALIZER.serialize(fixedKey);
        byte[] sharedKeyBytes = STRING_SERIALIZER.serialize(sharedKey);

        byte[] result = redisTemplate.execute((RedisConnection connection) -> {
            return connection.eval(
                    ACQUIRE_SEMAPHORE_LUA_BYTES,
                    ReturnType.VALUE,
                    2, // 두 개의 KEYS
                    fixedKeyBytes,
                    sharedKeyBytes,
                    STRING_SERIALIZER.serialize(String.valueOf(policy.getFixedLimit(aiType))),
                    STRING_SERIALIZER.serialize(String.valueOf(policy.getSharedLimit())),
                    STRING_SERIALIZER.serialize(requestId),
                    STRING_SERIALIZER.serialize(String.valueOf(SEMAPHORE_TTL.getSeconds()))
            );
        });

        if (result == null) {
            return false;
        }

        String resultStr = new String(result, StandardCharsets.UTF_8);
        switch (resultStr) {
            case "fixed", "shared", "duplicated" -> {
                log.info("세마포어 획득 성공: {}", resultStr + " " + requestId);
                return true;
            }
            case "none" -> {
//                log.info("세마포어 획득 실패: {}", requestId);
                return false;
            }
            default -> {
                log.warn("알 수 없는 세마포어 결과: {}", resultStr);
                return false;
            }
        }

    }

//    private boolean tryAcquire(String key, int limit, String requestId) {
//        Long result = redisTemplate.execute((RedisConnection connection) -> {
//            return connection.eval(
//                    ACQUIRE_SEMAPHORE_LUA_BYTES,
//                    ReturnType.INTEGER,
//                    1,
//                    STRING_SERIALIZER.serialize(key),
//                    STRING_SERIALIZER.serialize(String.valueOf(limit)),
//                    STRING_SERIALIZER.serialize(requestId),
//                    STRING_SERIALIZER.serialize(String.valueOf(SEMAPHORE_TTL.getSeconds()))
//            );
//        });
//
//        return result != null && result == 1;
//    }

    public void releaseSemaphore(String requestId, String resourceType) {
        redisTemplate.opsForSet()
                .remove(getFixedKey(resourceType), requestId);
        redisTemplate.opsForSet().remove(SHARED_SEMAPHORE_KEY, requestId);
    }

    private String getFixedKey(String resourceType) {
        return SEMAPHORE_KEY_PREFIX + resourceType + SEMAPHORE_KEY_SUFFIX;
    }

}

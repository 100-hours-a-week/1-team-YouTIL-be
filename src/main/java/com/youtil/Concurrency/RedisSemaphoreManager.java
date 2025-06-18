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
            "if redis.call('scard', KEYS[1]) < tonumber(ARGV[1]) then " +
                    "  redis.call('sadd', KEYS[1], ARGV[2]); " +
                    "  redis.call('expire', KEYS[1], ARGV[3]); " +
                    "  return 1; " +
                    "else " +
                    "  return 0; " +
                    "end";
    private static final byte[] ACQUIRE_SEMAPHORE_LUA_BYTES = ACQUIRE_SEMAPHORE_LUA.getBytes(
            StandardCharsets.UTF_8);
    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    private static final String SHARED_SEMAPHORE_KEY = "semaphore:shared";
    private static final String SEMAPHORE_KEY_PREFIX = "semaphore:";
    private static final String SEMAPHORE_KEY_SUFFIX = ":fixed";
    private static final Duration SEMAPHORE_TTL = Duration.ofMinutes(5);
    ;
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

        boolean acquiredFixed = tryAcquire(getFixedKey(resourceType), policy.getFixedLimit(aiType),
                requestId);

        //전용자원이 없을 경우, 공유 자원에 접근한다.
        return acquiredFixed || tryAcquire(SHARED_SEMAPHORE_KEY, policy.getSharedLimit(),
                requestId);

    }


    private boolean tryAcquire(String key, int limit, String requestId) {
        Long result = redisTemplate.execute((RedisConnection connection) -> {
            return connection.eval(
                    ACQUIRE_SEMAPHORE_LUA_BYTES,
                    ReturnType.INTEGER,
                    1,
                    STRING_SERIALIZER.serialize(key),
                    STRING_SERIALIZER.serialize(String.valueOf(limit)),
                    STRING_SERIALIZER.serialize(requestId),
                    STRING_SERIALIZER.serialize(String.valueOf(SEMAPHORE_TTL.getSeconds()))
            );
        });

        return result != null && result == 1;
    }

    public void releaseSemaphore(String requestId, String resourceType) {
        redisTemplate.opsForSet()
                .remove(getFixedKey(resourceType), requestId);
        redisTemplate.opsForSet().remove(SHARED_SEMAPHORE_KEY, requestId);
    }

    private String getFixedKey(String resourceType) {
        return SEMAPHORE_KEY_PREFIX + resourceType + SEMAPHORE_KEY_SUFFIX;
    }

}

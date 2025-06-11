package com.youtil.Util;


import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_TTL;
import com.youtil.Common.Enums.AiType;
import com.youtil.Exception.CommonException.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
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
    private static final int TIL_CPU_SEMAPHORE_COUNT = 1;
    private static final int TIL_GPU_SEMAPHORE_COUNT = 2;
    private static final int INTERVIEW_CPU_SEMAPHORE_COUNT = 1;
    private static final int INTERVIEW_GPU_SEMAPHORE_COUNT = 3;
    private static final int SHARED_CPU_SEMAPHORE_COUNT = 1;
    private static final int SHARED_GPU_SEMAPHORE_COUNT = 5;
    private static final String SEMAPHORE_KEY_PREFIX = "semaphore:";
    private static final String SEMAPHORE_KEY_SUFFIX = ":fixed";
    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquireSemaphore(String requestId, String resourceType) {
        String fixedKey = getFixedKey(resourceType);

        int fixedLimit = getFixedLimit(resourceType);
        int sharedLimit = getSharedLimit();

        // 고정 자원이 가능한지 확인한다.
        boolean acquiredFixed = tryAcquire(fixedKey, fixedLimit, requestId);

        if (acquiredFixed) {
            return true;
        }

        // 공유 자원 사용이 가능한지 확인한다.
        return tryAcquire(SHARED_SEMAPHORE_KEY, sharedLimit, requestId);
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

    private int getFixedLimit(String resourceType) {

        boolean isGpuTime = GpuTimeChecker.isGpuTimeNow();

        if (resourceType.equals(AiType.TIL.toString())) {
            return isGpuTime ? TIL_GPU_SEMAPHORE_COUNT : TIL_CPU_SEMAPHORE_COUNT;
        } else if (resourceType.equals(AiType.INTERVIEW.toString())) {
            return isGpuTime ? INTERVIEW_GPU_SEMAPHORE_COUNT : INTERVIEW_CPU_SEMAPHORE_COUNT;
        }
        throw new ResourceNotFoundException();
    }

    private int getSharedLimit() {
        return GpuTimeChecker.isGpuTimeNow()
                ? SHARED_GPU_SEMAPHORE_COUNT
                : SHARED_CPU_SEMAPHORE_COUNT;
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

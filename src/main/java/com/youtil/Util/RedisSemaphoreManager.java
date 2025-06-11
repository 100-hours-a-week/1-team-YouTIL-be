package com.youtil.Util;


import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_TTL;
import com.youtil.Common.Enums.AiType;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    private final String SHARED_SEMAPHORE_KEY = "semaphore:shared";
    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquireSemaphore(String requestId, String resourceType) {
        String fixedKey = "semaphore:" + resourceType + ":fixed";

        int fixedLimit = getFixedLimit(resourceType);
        int sharedLimit = getSharedLimit();

        // 고정 자원이 가능한지 확인한다.
        boolean acquiredFixed = tryAcquire(fixedKey, fixedLimit, requestId);

        if (acquiredFixed) {
            return true;
        }

        // 2순위: 공유 슬롯
        boolean acquiredShared = tryAcquire(SHARED_SEMAPHORE_KEY, sharedLimit, requestId);
        return acquiredShared;
    }


    private boolean tryAcquire(String key, int limit, String requestId) {
        Long result = redisTemplate.execute((RedisConnection connection) -> {
            return connection.eval(
                    ACQUIRE_SEMAPHORE_LUA.getBytes(),
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
        LocalTime currentTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime();
        boolean isGpuTime = currentTime.isAfter(LocalTime.of(15, 0));

        if (resourceType.equals(AiType.TIL.toString())) {
            return isGpuTime ? 2 : 1;
        } else if (resourceType.equals(AiType.INTERVIEW.toString())) {
            return isGpuTime ? 3 : 1;
        }
        throw new IllegalArgumentException("Unknown resource type");
    }

    private int getSharedLimit() {
        LocalTime currentTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime();
        return currentTime.isAfter(LocalTime.of(15, 0)) ? 5 : 1;
    }

    public void releaseSemaphore(String requestId, String resourceType) {
        redisTemplate.opsForSet().remove("semaphore:" + resourceType + ":fixed", requestId);
        redisTemplate.opsForSet().remove(SHARED_SEMAPHORE_KEY, requestId);
    }


}

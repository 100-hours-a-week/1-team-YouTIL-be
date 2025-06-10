package com.youtil.Util;



import static com.youtil.Common.Constants.TilServiceConstants.MAX_CPU_CONCURRENCY;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_GPU_CONCURRENCY;
import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_TTL;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.youtil.Common.Constants.InterviewServiceConstans.*;

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

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquireSemaphore(String requestId) {

       Long result = redisTemplate.execute((RedisConnection connection) -> {
        return connection.eval(
            ACQUIRE_SEMAPHORE_LUA.getBytes(),
            ReturnType.INTEGER,
            1,
            STRING_SERIALIZER.serialize(SEMAPHORE_KEY),
            STRING_SERIALIZER.serialize(String.valueOf(selectMaxSemaphore())),
            STRING_SERIALIZER.serialize(requestId),
            STRING_SERIALIZER.serialize(String.valueOf(SEMAPHORE_TTL.getSeconds()))
        );
    });

    return result != null && result == 1;
}

    public void releaseSemaphore(String requestId) {
        redisTemplate.opsForSet().remove(SEMAPHORE_KEY, requestId);
    }

    public int selectMaxSemaphore() {
        ZonedDateTime koreaTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = koreaTime.toLocalTime();

        LocalTime afternoonThree = LocalTime.of(15, 0); // 오후 3시

        // 오후 3시부터 자정까지는 primary 서버 사용
        if (currentTime.isAfter(afternoonThree) || currentTime.equals(afternoonThree)) {

            return MAX_GPU_CONCURRENCY;
        } else {
            return MAX_CPU_CONCURRENCY;
        }

    }
}

package com.youtil.Util;


import static com.youtil.Common.Constants.TilServiceConstants.MAX_CPU_CONCURRENCY;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_GPU_CONCURRENCY;
import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.SEMAPHORE_TTL;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSemaphoreManager {


    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquireSemaphore(String requestId) {
        Long current = redisTemplate.opsForSet().size(SEMAPHORE_KEY);

        int maxConCurrency = selectMaxSemaphore();
//        log.info("현재 동시 개수 : {}", maxConCurrency);
        if (current == null || current >= maxConCurrency) {
            return false;
        }

        redisTemplate.opsForSet().add(SEMAPHORE_KEY, requestId);
        // Optional TTL 키 보조 설정 (비정상 종료 대비)
        redisTemplate.expire(SEMAPHORE_KEY, SEMAPHORE_TTL);
        return true;
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

package com.youtil.Api.Community.Scheduler;


import com.youtil.Repository.CommentRepository;
import com.youtil.Repository.TilRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityScheduler {

    private static final String KEY = "changed:tils";
    private final StringRedisTemplate redisTemplate;
    private final TilRepository tilRepository;
    private final CommentRepository commentRepository;

    @Scheduled(fixedDelayString = "${sync.delay.ms:30000}")
    @SchedulerLock(name = "CommunityScheduler_sync", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    @Transactional
    public void syncChangedPosts() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        long now = System.currentTimeMillis();
        Set<String> tilIdStrSet = zSetOperations.rangeByScore(KEY, 0, now);
        if (tilIdStrSet == null || tilIdStrSet.isEmpty()) {
            return;
        }

        Set<Long> tilIds = tilIdStrSet.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());

        for (Long tilId : tilIds) {
            String likeKey = "til:" + tilId + ":like_count";
            String commentKey = "til:" + tilId + ":comment_count";
            String viewKey = "til:" + tilId + ":visit_count";
            
            int likes = Optional.ofNullable(redisTemplate.opsForValue().get(likeKey))
                    .map(Integer::parseInt).orElse(0);
            int comments = Optional.ofNullable(redisTemplate.opsForValue().get(commentKey))
                    .map(Integer::parseInt).orElse(0);
            int views = Optional.ofNullable(redisTemplate.opsForValue().get(viewKey))
                    .map(Integer::parseInt).orElse(0);

            tilRepository.updateCounts(tilId, likes, comments, views);
        }
    }
}

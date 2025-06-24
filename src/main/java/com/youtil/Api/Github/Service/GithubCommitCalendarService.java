package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO.CommitCalendarResponse;
import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO.PeriodInfo;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Model.User;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitCalendarService {

    private final WebClient webClient;
    private final EntityValidator entityValidator;
    private final GitHubApiUtils gitHubApiUtils;
    private final StringRedisTemplate redisTemplate;

    private static final String COMMIT_CALENDAR_HASH_PATTERN = "commit_calendar:user:%d:repo:%d:branch:%s:%s";
    private static final Duration CACHE_TTL = Duration.ofDays(30); // 30일 TTL

    /**
     * Hash 구조를 사용한 최적화된 커밋 달력 조회 (StringRedisTemplate 버전)
     */
    public CommitCalendarResponse getCommitCalendar(
            Long userId, Long organizationId, Long repositoryId, String branch,
            LocalDate startDate, LocalDate endDate) {

        long startTime = System.currentTimeMillis();
        log.info("StringRedisTemplate Hash 구조 커밋 달력 조회 시작: 사용자={}, 레포={}, 브랜치={}, 기간={} ~ {}",
                userId, repositoryId, branch, startDate, endDate);

        // 사용자 조회 및 토큰 검증
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);
        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        // 레포지토리 정보 조회
        Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(repositoryId, token);
        String repoName = repoInfo.get("name").toString();
        String owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();
        String username = gitHubApiUtils.getUsernameFromToken(token);

        log.info("레포지토리 정보: 소유자={}, 레포명={}", owner, repoName);

        // 1단계: 날짜를 월별로 그룹화
        Map<String, List<String>> monthlyDates = groupDatesByMonth(startDate, endDate);
        log.info("조회 대상 월: {}, 총 날짜 수: {}", monthlyDates.keySet(),
                monthlyDates.values().stream().mapToInt(List::size).sum());

        // 2단계: Redis Hash에서 월별 배치 조회 (StringRedisTemplate)
        Map<String, Integer> cachedCommits = new HashMap<>();
        Set<String> missedDates = new HashSet<>();

        batchFetchFromRedisHash(userId, repositoryId, branch, monthlyDates, cachedCommits, missedDates);

        log.info("캐시 분석 결과: 히트={}, 미스={}", cachedCommits.size(), missedDates.size());

        // 3단계: 캐시 미스된 날짜들에 대해 GitHub API 호출
        if (!missedDates.isEmpty()) {
            checkMissedDatesAndSaveToHash(missedDates, userId, repositoryId, branch, owner, repoName,
                    token, username, cachedCommits);
        }

        // 4단계: 날짜 내림차순으로 정렬된 calendar 생성
        Map<String, Integer> sortedCalendar = cachedCommits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByKey().reversed())
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);

        // 5단계: 응답 생성
        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count();
        PeriodInfo periodInfo = PeriodInfo.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalDays(totalDays)
                .commitDays(sortedCalendar.size())
                .build();

        long duration = System.currentTimeMillis() - startTime;
        log.info("StringRedisTemplate Hash 구조 커밋 달력 조회 완료: {}일 중 {}일에 커밋 존재, 소요시간={}ms (내림차순 정렬 적용)",
                totalDays, sortedCalendar.size(), duration);

        return CommitCalendarResponse.builder()
                .username(username)
                .repo(repoName)
                .owner(owner)
                .branch(branch)
                .calendar(sortedCalendar)
                .period(periodInfo)
                .build();
    }

    /**
     * 날짜를 년월별로 그룹화
     */
    private Map<String, List<String>> groupDatesByMonth(LocalDate startDate, LocalDate endDate) {
        Map<String, List<String>> monthlyDates = new LinkedHashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String yearMonth = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthlyDates.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(currentDate.toString());
            currentDate = currentDate.plusDays(1);
        }

        log.debug("월별 그룹화 완료: {} 개월", monthlyDates.size());
        return monthlyDates;
    }

    /**
     * Redis Hash에서 월별 배치 조회 (StringRedisTemplate 버전)
     */
    private void batchFetchFromRedisHash(Long userId, Long repositoryId, String branch,
                                         Map<String, List<String>> monthlyDates,
                                         Map<String, Integer> cachedCommits, Set<String> missedDates) {

        for (Map.Entry<String, List<String>> entry : monthlyDates.entrySet()) {
            String yearMonth = entry.getKey();
            List<String> datesInMonth = entry.getValue();

            String hashKey = String.format(COMMIT_CALENDAR_HASH_PATTERN, userId, repositoryId, branch, yearMonth);

            // StringRedisTemplate Hash에서 해당 월의 모든 필드(날짜) 조회
            // 모든 값이 String으로 반환됨
            Map<Object, Object> monthData = redisTemplate.opsForHash().entries(hashKey);

            for (String date : datesInMonth) {
                Object cachedValue = monthData.get(date);

                if (cachedValue != null) {
                    try {
                        // String 값을 int로 파싱
                        String valueStr = cachedValue.toString();
                        int value = Integer.parseInt(valueStr);
                        if (value == 1) {
                            cachedCommits.put(date, 1); // 커밋 있는 날짜만 응답에 포함
                        }
                        log.debug("StringRedisTemplate Hash 캐시 히트: {} -> {}", date, value);
                    } catch (NumberFormatException e) {
                        log.warn("StringRedisTemplate Hash 캐시 값 파싱 오류: date={}, value={}", date, cachedValue);
                        missedDates.add(date);
                    }
                } else {
                    missedDates.add(date);
                    log.debug("StringRedisTemplate Hash 캐시 미스: {}", date);
                }
            }

            log.debug("월별 StringRedisTemplate Hash 조회 완료: {} - 캐시된 날짜: {}/{}",
                    yearMonth, monthData.size(), datesInMonth.size());
        }
    }

    /**
     * 미스된 날짜들을 GitHub에서 조회하고 Hash에 배치 저장 (StringRedisTemplate 버전)
     */
    private void checkMissedDatesAndSaveToHash(Set<String> missedDates, Long userId, Long repositoryId,
                                               String branch, String owner, String repoName, String token,
                                               String username, Map<String, Integer> cachedCommits) {

        log.info("GitHub API 호출 시작: {}개 날짜 확인", missedDates.size());

        Map<String, Map<String, String>> monthlyUpdates = new HashMap<>();

        for (String date : missedDates) {
            try {
                boolean hasCommits = checkCommitsForDate(owner, repoName, branch, date, token, username);

                String yearMonth = date.substring(0, 7);
                monthlyUpdates.computeIfAbsent(yearMonth, k -> new HashMap<>())
                        .put(date, hasCommits ? "1" : "0");

                if (hasCommits) {
                    cachedCommits.put(date, 1); // 응답에 포함
                    log.debug("커밋 발견: {}", date);
                } else {
                    log.debug("커밋 없음: {}", date);
                }

            } catch (Exception e) {
                log.warn("날짜 {} 커밋 확인 실패: {}", date, e.getMessage());
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : monthlyUpdates.entrySet()) {
            String yearMonth = entry.getKey();
            Map<String, String> updates = entry.getValue();

            String hashKey = String.format(COMMIT_CALENDAR_HASH_PATTERN, userId, repositoryId, branch, yearMonth);

            // 날짜 내림차순으로 정렬하여 저장
            Map<String, String> sortedUpdates = updates.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByKey().reversed()) // 날짜 내림차순 정렬
                    .collect(LinkedHashMap::new,
                            (map, updateEntry) -> map.put(updateEntry.getKey(), updateEntry.getValue()),
                            LinkedHashMap::putAll);

            // StringRedisTemplate Hash에 정렬된 순서로 배치 저장
            redisTemplate.opsForHash().putAll(hashKey, sortedUpdates);
            redisTemplate.expire(hashKey, CACHE_TTL.getSeconds(), TimeUnit.SECONDS);

            log.info("StringRedisTemplate Redis Hash 배치 저장 완료 (내림차순 정렬): key={}, 업데이트된 날짜 수={}, TTL={}일",
                    hashKey, sortedUpdates.size(), CACHE_TTL.toDays());
        }
    }

    /**
     * 특정 날짜에 커밋이 있는지 확인
     */
    private boolean checkCommitsForDate(String owner, String repo, String branch, String date,
                                        String token, String authorUsername) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            LocalDateTime startDateTime = targetDate.atStartOfDay();
            LocalDateTime endDateTime = targetDate.plusDays(1).atStartOfDay();

            String sinceIso = startDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

            // GitHub API 호출 (첫 번째 커밋만 확인하면 되므로 per_page=1)
            String commitsUrl = String.format(
                    "https://api.github.com/repos/%s/%s/commits?sha=%s&since=%s&until=%s&author=%s&per_page=1",
                    owner, repo, branch, sinceIso, untilIso, authorUsername);

            log.debug("GitHub API 호출: {}", commitsUrl);

            Map<String, Object>[] commits = webClient.get()
                    .uri(commitsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            boolean hasCommits = commits != null && commits.length > 0;
            log.debug("날짜 {} 커밋 확인 결과: {}", date, hasCommits);

            return hasCommits;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                // 브랜치나 레포지토리를 찾을 수 없는 경우
                log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
                return false;
            }
            throw new RuntimeException("GitHub API 호출 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("날짜 {} 커밋 확인 중 오류: {}", date, e.getMessage());
            throw new RuntimeException("커밋 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

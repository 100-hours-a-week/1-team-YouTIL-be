package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO;
import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO.CommitCalendarResponse;
import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO.PeriodInfo;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitCalendarService {

    private final WebClient webClient;
    private final EntityValidator entityValidator;
    private final GitHubApiUtils gitHubApiUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COMMIT_EXISTS_KEY_PATTERN = "commit_exists:user:%d:repo:%d:branch:%s:date:%s";
    private static final Duration CACHE_TTL = Duration.ofDays(30); // 30일 TTL

    /**
     * 3개월간의 커밋 존재 여부 달력 데이터를 조회합니다.
     */
    public CommitCalendarResponse getCommitCalendar(
            Long userId, Long organizationId, Long repositoryId, String branch,
            LocalDate startDate, LocalDate endDate) {

        long startTime = System.currentTimeMillis();
        log.info("커밋 달력 조회 시작: 사용자={}, 레포={}, 브랜치={}, 기간={} ~ {}",
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

        // 1단계: 날짜 범위 생성
        List<String> dateList = generateDateRange(startDate, endDate);
        log.info("조회할 날짜 수: {}일", dateList.size());

        // 2단계: Redis 배치 조회
        List<String> redisKeys = generateRedisKeys(userId, repositoryId, branch, dateList);
        List<Object> cachedValues = redisTemplate.opsForValue().multiGet(redisKeys);

        // 3단계: 캐시 분석 및 미스된 날짜 수집
        Map<String, Integer> cachedCommits = new HashMap<>();
        List<String> missedDates = new ArrayList<>();

        analyzeCacheResults(dateList, cachedValues, cachedCommits, missedDates);

        log.info("캐시 분석 결과: 히트={}, 미스={}", cachedCommits.size(), missedDates.size());

        // 4단계: 캐시 미스된 날짜들에 대해 GitHub API 호출
        if (!missedDates.isEmpty()) {
            checkMissedDatesFromGitHub(missedDates, userId, repositoryId, branch, owner, repoName,
                    token, username, cachedCommits);
        }

        // 5단계: 응답 생성
        PeriodInfo periodInfo = PeriodInfo.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalDays(dateList.size())
                .commitDays(cachedCommits.size())
                .build();

        long duration = System.currentTimeMillis() - startTime;
        log.info("커밋 달력 조회 완료: {}일 중 {}일에 커밋 존재, 소요시간={}ms",
                dateList.size(), cachedCommits.size(), duration);

        return CommitCalendarResponse.builder()
                .username(username)
                .repo(repoName)
                .owner(owner)
                .branch(branch)
                .calendar(cachedCommits)
                .period(periodInfo)
                .build();
    }

    /**
     * 날짜 범위 생성
     */
    private List<String> generateDateRange(LocalDate startDate, LocalDate endDate) {
        List<String> dateList = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            dateList.add(currentDate.toString());
            currentDate = currentDate.plusDays(1);
        }

        return dateList;
    }

    /**
     * Redis 키 생성
     */
    private List<String> generateRedisKeys(Long userId, Long repositoryId, String branch, List<String> dateList) {
        return dateList.stream()
                .map(date -> String.format(COMMIT_EXISTS_KEY_PATTERN, userId, repositoryId, branch, date))
                .collect(Collectors.toList());
    }

    /**
     * 캐시 결과 분석
     */
    private void analyzeCacheResults(List<String> dateList, List<Object> cachedValues,
                                     Map<String, Integer> cachedCommits, List<String> missedDates) {
        for (int i = 0; i < dateList.size(); i++) {
            String date = dateList.get(i);
            Object cachedValue = cachedValues.get(i);

            if (cachedValue != null) {
                // 캐시 히트: 이미 조회한 날짜
                int value = Integer.parseInt(cachedValue.toString());
                if (value == 1) {
                    cachedCommits.put(date, 1); // 커밋 있는 날짜만 응답에 포함
                }
                // value == 0이면 커밋 없는 날짜 (응답에서 제외, API 호출도 안함)
            } else {
                // 캐시 미스: 아직 조회 안한 날짜
                missedDates.add(date);
            }
        }
    }

    /**
     * 캐시 미스된 날짜들에 대해 GitHub API 호출
     */
    private void checkMissedDatesFromGitHub(List<String> missedDates, Long userId, Long repositoryId,
                                            String branch, String owner, String repoName, String token,
                                            String username, Map<String, Integer> cachedCommits) {
        log.info("GitHub API 호출 시작: {}개 날짜 확인", missedDates.size());

        for (String date : missedDates) {
            try {
                boolean hasCommits = checkCommitsForDate(owner, repoName, branch, date, token, username);

                // 조회 결과를 무조건 저장 (0 또는 1)
                if (hasCommits) {
                    saveToRedis(userId, repositoryId, branch, date, 1);
                    cachedCommits.put(date, 1); // 응답에 포함
                    log.debug("커밋 발견: {}", date);
                } else {
                    saveToRedis(userId, repositoryId, branch, date, 0);
                    // cachedCommits에는 추가하지 않음 (응답에서 제외)
                    log.debug("커밋 없음: {}", date);
                }
            } catch (Exception e) {
                log.warn("날짜 {} 커밋 확인 실패: {}", date, e.getMessage());
                // 오류 발생시 해당 날짜는 캐싱하지 않음 (다음에 다시 시도)
            }
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

            Map<String, Object>[] commits = webClient.get()
                    .uri(commitsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            return commits != null && commits.length > 0;

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

    /**
     * Redis에 커밋 존재 여부 저장
     */
    private void saveToRedis(Long userId, Long repositoryId, String branch, String date, int value) {
        String key = String.format(COMMIT_EXISTS_KEY_PATTERN, userId, repositoryId, branch, date);
        redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        log.info("Redis 저장 완료: key=[{}], value=[{}], TTL=[{}일]", key, value, CACHE_TTL.toDays());
    }

    /**
     * Redis 키 확인용 디버그 메서드 (개발용)
     */
    public void debugRedisKeys(Long userId, Long repositoryId, String branch) {
        String pattern = String.format("commit_exists:user:%d:repo:%d:branch:%s:*", userId, repositoryId, branch);
        Set<String> keys = redisTemplate.keys(pattern);

        log.info("=== Redis 키 디버그 정보 ===");
        log.info("검색 패턴: {}", pattern);
        log.info("찾은 키 개수: {}", keys != null ? keys.size() : 0);

        if (keys != null && !keys.isEmpty()) {
            keys.forEach(key -> {
                Object value = redisTemplate.opsForValue().get(key);
                log.info("키: {} -> 값: {}", key, value);
            });
        } else {
            log.info("해당 패턴의 키가 존재하지 않습니다.");
        }
        log.info("========================");
    }
}

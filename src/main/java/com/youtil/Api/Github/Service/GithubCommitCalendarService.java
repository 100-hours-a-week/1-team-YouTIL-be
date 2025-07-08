package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Constants.GitHubApiConstants;
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
import java.util.stream.Collectors;

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
    private static final int MAX_PER_PAGE = 100;

    /**
     * Hash 구조를 사용한 최적화된 커밋 달력 조회 (연도 전체)
     */
    public CommitCalendarResponse getCommitCalendar(
            Long userId, Long organizationId, Long repositoryId, String branchId,
            LocalDate startDate, LocalDate endDate) {

        long startTime = System.currentTimeMillis();
        log.info("커밋 달력 조회 시작: 사용자={}, 레포={}, 브랜치={}, {}년 전체",
                userId, repositoryId, branchId, startDate.getYear());

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

        // 날짜를 월별로 그룹화
        Map<String, List<String>> monthlyDates = groupDatesByMonth(startDate, endDate);
        log.info("조회 대상 월: {}, 총 날짜 수: {}", monthlyDates.keySet(),
                monthlyDates.values().stream().mapToInt(List::size).sum());

        // Redis Hash에서 월별 배치 조회
        Map<String, Integer> cachedCommits = new HashMap<>();
        Set<String> missedDates = new HashSet<>();

        batchFetchFromRedisHash(userId, repositoryId, branchId, monthlyDates, cachedCommits, missedDates);

        log.info("캐시 분석 결과: 히트={}, 미스={}", cachedCommits.size(), missedDates.size());

        // 캐시 미스된 날짜들에 대해 배치 처리로 GitHub API 호출
        if (!missedDates.isEmpty()) {
            checkMissedDatesAndSaveToHash(missedDates, userId, repositoryId, branchId, owner, repoName,
                    token, username, cachedCommits);
        }

        // 날짜 내림차순으로 정렬된 calendar 생성
        Map<String, Integer> sortedCalendar = cachedCommits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByKey().reversed())
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);

        // 응답 생성
        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count();
        PeriodInfo periodInfo = PeriodInfo.builder()
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalDays(totalDays)
                .commitDays(sortedCalendar.size())
                .build();

        long duration = System.currentTimeMillis() - startTime;
        log.info("커밋 달력 조회 완료: {}년 전체 {}일 중 {}일에 커밋 존재, 소요시간={}ms",
                startDate.getYear(), totalDays, sortedCalendar.size(), duration);

        return CommitCalendarResponse.builder()
                .username(username)
                .repo(repoName)
                .owner(owner)
                .branch(branchId)
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
     * Redis Hash에서 월별 배치 조회
     */
    private void batchFetchFromRedisHash(Long userId, Long repositoryId, String branchId,
                                         Map<String, List<String>> monthlyDates,
                                         Map<String, Integer> cachedCommits, Set<String> missedDates) {

        for (Map.Entry<String, List<String>> entry : monthlyDates.entrySet()) {
            String yearMonth = entry.getKey();
            List<String> datesInMonth = entry.getValue();

            String hashKey = String.format(COMMIT_CALENDAR_HASH_PATTERN, userId, repositoryId, branchId, yearMonth);
            Map<Object, Object> monthData = redisTemplate.opsForHash().entries(hashKey);

            for (String date : datesInMonth) {
                Object cachedValue = monthData.get(date);

                if (cachedValue != null) {
                    try {
                        String valueStr = cachedValue.toString();
                        int value = Integer.parseInt(valueStr);
                        if (value == 1) {
                            cachedCommits.put(date, 1);
                        }
                        log.debug("캐시 히트: {} -> {}", date, value);
                    } catch (NumberFormatException e) {
                        log.warn("캐시 값 파싱 오류: date={}, value={}", date, cachedValue);
                        missedDates.add(date);
                    }
                } else {
                    missedDates.add(date);
                    log.debug("캐시 미스: {}", date);
                }
            }

            log.debug("월별 캐시 조회 완료: {} - 캐시된 날짜: {}/{}",
                    yearMonth, monthData.size(), datesInMonth.size());
        }
    }

    /**
     * 미스된 날짜들을 GitHub에서 조회하고 Hash에 배치 저장
     */
    private void checkMissedDatesAndSaveToHash(Set<String> missedDates, Long userId, Long repositoryId,
                                               String branchId, String owner, String repoName, String token,
                                               String username, Map<String, Integer> cachedCommits) {

        log.info("GitHub API 호출 시작: {}개 날짜 확인", missedDates.size());

        // 연속된 날짜 범위들로 그룹화
        List<DateRange> dateRanges = groupConsecutiveDates(missedDates);
        log.info("날짜 범위 그룹화: {}개 범위로 최적화", dateRanges.size());

        Map<String, Map<String, String>> monthlyUpdates = new HashMap<>();

        // 각 날짜 범위별로 배치 API 호출
        for (DateRange range : dateRanges) {
            try {
                Map<String, Boolean> rangeResults = batchCheckCommitsForDateRange(
                        owner, repoName, branchId, range.start, range.end, token, username);

                // 결과를 월별로 정리
                for (Map.Entry<String, Boolean> entry : rangeResults.entrySet()) {
                    String date = entry.getKey();
                    boolean hasCommits = entry.getValue();

                    String yearMonth = date.substring(0, 7);
                    monthlyUpdates.computeIfAbsent(yearMonth, k -> new HashMap<>())
                            .put(date, hasCommits ? "1" : "0");

                    if (hasCommits) {
                        cachedCommits.put(date, 1);
                        log.debug("커밋 발견: {}", date);
                    } else {
                        log.debug("커밋 없음: {}", date);
                    }
                }

                log.info("범위 {} 처리 완료: {}개 날짜", range.toString(), rangeResults.size());

            } catch (Exception e) {
                log.warn("날짜 범위 {} 처리 실패: {}", range.toString(), e.getMessage());
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : monthlyUpdates.entrySet()) {
            String yearMonth = entry.getKey();
            Map<String, String> updates = entry.getValue();

            String hashKey = String.format(COMMIT_CALENDAR_HASH_PATTERN, userId, repositoryId, branchId, yearMonth);

            // 날짜 내림차순으로 정렬하여 저장
            Map<String, String> sortedUpdates = updates.entrySet().stream()
                    .sorted(Map.Entry.<String, String>comparingByKey().reversed()) // 날짜 내림차순 정렬
                    .collect(LinkedHashMap::new,
                            (map, updateEntry) -> map.put(updateEntry.getKey(), updateEntry.getValue()),
                            LinkedHashMap::putAll);

            redisTemplate.opsForHash().putAll(hashKey, sortedUpdates);
            redisTemplate.expire(hashKey, CACHE_TTL.getSeconds(), TimeUnit.SECONDS);

            log.info("Redis 배치 저장 완료: key={}, 날짜 수={}, TTL={}일",
                    hashKey, sortedUpdates.size(), CACHE_TTL.toDays());
        }
    }

    /**
     * 날짜 범위에 대해 단일 API 호출로 모든 커밋 조회
     */
    private Map<String, Boolean> batchCheckCommitsForDateRange(String owner, String repo, String branchId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               String token, String authorUsername) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        String sinceIso = startDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        Map<String, Boolean> dateCommitMap = new HashMap<>();

        // 범위 내 모든 날짜를 false로 초기화
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dateCommitMap.put(currentDate.toString(), false);
            currentDate = currentDate.plusDays(1);
        }

        int page = 1;
        int totalCommitsProcessed = 0;

        log.info("배치 API 호출 시작: 범위={} ~ {}", startDate, endDate);

        while (true) {
            // GitHubApiConstants를 사용하여 URL 구성
            String commitsUrl = GitHubApiConstants.REPOS_BASE_URL + owner + "/" + repo + GitHubApiConstants.COMMITS_PATH
                    + "?sha=" + branchId
                    + "&since=" + sinceIso
                    + "&until=" + untilIso
                    + "&author=" + authorUsername
                    + "&page=" + page
                    + "&per_page=" + MAX_PER_PAGE;

            log.debug("GitHub API 호출: page={}", page);

            try {
                Map<String, Object>[] commits = webClient.get()
                        .uri(commitsUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map[].class)
                        .block();

                if (commits == null || commits.length == 0) {
                    break;
                }

                // 각 커밋의 날짜 추출하여 해당 날짜를 true로 마킹
                for (Map<String, Object> commit : commits) {
                    try {
                        Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
                        Map<String, Object> committer = (Map<String, Object>) commitData.get("committer");
                        String commitDateStr = committer.get("date").toString();

                        // 날짜만 추출 (YYYY-MM-DD)
                        LocalDate commitDate = LocalDate.parse(commitDateStr.substring(0, 10));
                        String dateKey = commitDate.toString();

                        if (dateCommitMap.containsKey(dateKey)) {
                            dateCommitMap.put(dateKey, true);
                            totalCommitsProcessed++;
                        }

                    } catch (Exception e) {
                        log.warn("커밋 날짜 파싱 오류: {}", e.getMessage());
                    }
                }

                if (commits.length < MAX_PER_PAGE) {
                    break;
                }
                page++;

            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() == 404) {
                    log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
                    break;
                }
                throw new RuntimeException("GitHub API 호출 실패: " + e.getMessage());
            }
        }

        log.info("배치 조회 완료: {}페이지, {}개 커밋 처리, {}일에 커밋 존재",
                page - 1, totalCommitsProcessed,
                dateCommitMap.values().stream().mapToInt(b -> b ? 1 : 0).sum());

        return dateCommitMap;
    }

    /**
     * 연속된 날짜들을 범위로 그룹화하여 API 호출 최소화
     */
    private List<DateRange> groupConsecutiveDates(Set<String> missedDates) {
        List<LocalDate> sortedDates = missedDates.stream()
                .map(LocalDate::parse)
                .sorted()
                .collect(Collectors.toList());

        List<DateRange> ranges = new ArrayList<>();
        if (sortedDates.isEmpty()) {
            return ranges;
        }

        LocalDate rangeStart = sortedDates.get(0);
        LocalDate rangeEnd = sortedDates.get(0);

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate currentDate = sortedDates.get(i);

            // 연속된 날짜인지 확인 (하루 차이)
            if (rangeEnd.plusDays(1).equals(currentDate)) {
                rangeEnd = currentDate;
            } else {
                // 연속되지 않으면 이전 범위 저장하고 새 범위 시작
                ranges.add(new DateRange(rangeStart, rangeEnd));
                rangeStart = currentDate;
                rangeEnd = currentDate;
            }
        }

        // 마지막 범위 추가
        ranges.add(new DateRange(rangeStart, rangeEnd));

        log.info("날짜 그룹화 결과: {}개 날짜 -> {}개 범위", sortedDates.size(), ranges.size());
        return ranges;
    }

    /**
     * 날짜 범위를 나타내는 내부 클래스
     */
    private static class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start.equals(end) ? start.toString() : start + " ~ " + end;
        }
    }
}

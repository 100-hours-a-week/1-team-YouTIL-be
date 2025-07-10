package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Exception.TilException.TilException.TilAIHealthxception;
import com.youtil.Model.User;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final WebClient webClient;
    private final EntityValidator entityValidator;
    private final GitHubApiUtils gitHubApiUtils;

    @Value("${ai.api.url.primary}")
    private String primaryAiApiUrl;

    @Value("${ai.api.url.secondary}")
    private String secondaryAiApiUrl;

    /**
     * 현재 시간에 따라 적절한 AI 서버 URL을 반환합니다.
     * 한국 시간(KST) 기준으로 판단합니다.
     * 오후 3시(15:00) ~ 오전 12시(24:00/00:00) : primary 서버 사용
     * 오전 12시(00:00) ~ 오후 3시(15:00) : secondary 서버 사용
     */
    private String getActiveAiServerUrl() {
        // 한국 시간대로 현재 시간 가져오기
        ZonedDateTime koreaTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = koreaTime.toLocalTime();

        LocalTime afternoonThree = LocalTime.of(15, 0); // 오후 3시

        // 오후 3시부터 자정까지는 primary 서버 사용
        if (currentTime.isAfter(afternoonThree) || currentTime.equals(afternoonThree)) {
            log.debug("현재 한국 시간 {}로 primary AI 서버 사용: {}", currentTime, primaryAiApiUrl);
            return primaryAiApiUrl;
        }
        // 자정부터 오후 3시까지는 secondary 서버 사용
        else {
            log.debug("현재 한국 시간 {}로 secondary AI 서버 사용: {}", currentTime, secondaryAiApiUrl);
            return secondaryAiApiUrl;
        }
    }

    /**
     * TIL 내용을 생성합니다.
     */
    public TilAiResponseDTO generateTilContent(
            TilRequestDTO.CreateWithAiRequest request, Long userId) {

        // 현재 시간에 따른 AI 서버 URL 선택
        String currentAiApiUrl = getActiveAiServerUrl();

        log.info("AI API로 TIL 내용 생성 요청 [한국시간 기준] - 제목: {}, 브랜치: {}, 커밋 수: {}, 사용 중인 AI 서버 URL: {}",
                request.getTitle(),
                request.getBranch(),
                request.getCommits() != null ? request.getCommits().size() : 0,
                currentAiApiUrl);

        // 사용자 조회 및 토큰 검증
        entityValidator.getValidUserOrThrow(userId);

        // 레포지토리 정보 조회
        String owner;
        String repoName;
        String commitDate;

        try {
            // 레포지토리 정보 조회
            Map<String, Object> repoInfo = getRepositoryInfo(request.getRepositoryId(), userId);
            owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();
            repoName = repoInfo.get("name").toString();

            // 커밋 날짜 추출 (첫 번째 커밋 기준)
            commitDate = extractCommitDate(request, userId);

            log.info("레포지토리 정보: 소유자={}, 레포명={}, 커밋날짜={}", owner, repoName, commitDate);

        } catch (Exception e) {
            log.error("레포지토리 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("레포지토리 정보를 가져올 수 없습니다: " + e.getMessage());
        }

        // SHA 리스트 추출
        List<String> shaList = request.getCommits().stream()
                .map(TilRequestDTO.CommitSummary::getSha)
                .collect(Collectors.toList());

        // AI 요청 DTO 생성
        TilAiRequestDTO requestDTO = TilAiRequestDTO.builder()
                .owner(owner)
                .repo(repoName)
                .date(commitDate)
                .branch(request.getBranch())
                .sha_list(shaList)
                .build();

        // 요청 데이터 로깅
        log.info("AI 서버 요청: owner={}, repo={}, date={}, branch={}, sha_list={}",
                requestDTO.getOwner(), requestDTO.getRepo(), requestDTO.getDate(),
                requestDTO.getBranch(), requestDTO.getSha_list());

        String fullUrl = currentAiApiUrl + "/til";
        log.info("요청 전송 URL: {}", fullUrl);

        try {
            // WebClient를 사용하여 AI API 호출
            TilAiResponseDTO response = webClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDTO)
                    .retrieve()
                    .bodyToMono(TilAiResponseDTO.class)
                    .block();

            log.info("AI API 응답 수신 완료 (서버: {})", currentAiApiUrl);

            if (response == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다. (서버: {})", currentAiApiUrl);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서버에서 유효한 응답을 받지 못했습니다.");
            }

            log.info("AI 응답 내용: content 길이={}, tags={}",
                    response.getContent() != null ? response.getContent().length() : 0,
                    response.getKeywords());

            return response;

        } catch (WebClientResponseException e) {
            log.error("AI API 호출 실패 (서버: {}): {}", currentAiApiUrl, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버와의 연결이 원활하지 않습니다: " + e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 처리 중 예상치 못한 오류 발생 (서버: {}): {}", currentAiApiUrl, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 서비스 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 레포지토리 정보 조회
     */
    private Map<String, Object> getRepositoryInfo(Long repositoryId, Long userId) {
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);
        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        return gitHubApiUtils.getRepositoryById(repositoryId, token);
    }

    /**
     * 첫 번째 커밋에서 날짜 추출 (KST 기준)
     */
    private String extractCommitDate(TilRequestDTO.CreateWithAiRequest request, Long userId) {
        if (request.getCommits() == null || request.getCommits().isEmpty()) {
            // 커밋이 없으면 현재 날짜 반환
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        try {
            // 첫 번째 커밋의 실제 날짜를 GitHub에서 조회
            User user = entityValidator.getValidUserOrThrow(userId);
            String token = gitHubApiUtils.decryptToken(user.getGithubToken());

            // 레포지토리 정보 조회
            Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(request.getRepositoryId(), token);
            String owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();
            String repoName = repoInfo.get("name").toString();

            // 첫 번째 커밋의 SHA로 커밋 정보 조회
            String firstCommitSha = request.getCommits().get(0).getSha();
            String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s",
                    owner, repoName, firstCommitSha);

            Map<String, Object> commitInfo = webClient.get()
                    .uri(commitUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (commitInfo != null) {
                Map<String, Object> commit = (Map<String, Object>) commitInfo.get("commit");
                Map<String, Object> committer = (Map<String, Object>) commit.get("committer");
                String dateStr = committer.get("date").toString();

                // UTC -> KST 변환
                Instant instant = Instant.parse(dateStr);
                String kstDate = instant.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();
                return kstDate;
            }
        } catch (Exception e) {
            log.warn("커밋 날짜 추출 실패: {}", e.getMessage());
        }

        // 실패 시 현재 날짜 반환
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * AI 서버 헬스 체크
     * 현재 활성화된 서버의 헬스를 체크합니다.
     */
    public String getTilAIHealthStatus() {
        String currentAiApiUrl = getActiveAiServerUrl();
        String fullUrl = currentAiApiUrl + "/health";

        log.info("AI 서버 헬스 체크 [한국시간 기준] (서버: {})", currentAiApiUrl);

        try {
            return webClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new TilAIHealthxception())
                    )
                    .bodyToMono(String.class)
                    .block(); // 동기 호출
        } catch (Exception e) {
            log.error("AI 서버 헬스 체크 실패 (서버: {}): {}", currentAiApiUrl, e.getMessage());
            throw new TilAIHealthxception();
        }
    }
}

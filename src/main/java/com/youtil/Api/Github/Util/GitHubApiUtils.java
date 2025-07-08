package com.youtil.Api.Github.Util;

import com.youtil.Exception.GithubException.GitHubExceptions.*;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Api.Github.Constants.GitHubApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * GitHub API 호출 관련 공통 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubApiUtils {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;

    /**
     * 사용자의 GitHub 토큰이 있는지 확인합니다.
     */
    public void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new GitHubTokenException("GitHub 토큰이 설정되지 않았습니다.");
        }
    }

    /**
     * 레포지토리 ID로 직접 레포지토리 정보를 조회합니다.
     * 조직/개인 레포지토리 구분 없이 사용자가 접근 가능한 레포지토리를 조회합니다.
     */
    public Map<String, Object> getRepositoryById(Long repositoryId, String token) {
        Map<String, Object> repository = callGitHubApi(
                webClient.get()
                        .uri(GitHubApiConstants.REPOSITORIES_BASE_URL + repositoryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map.class),
                "레포지토리 정보 조회"
        );

        if (repository == null || !repository.containsKey("name") || !repository.containsKey("owner")) {
            throw new GitHubApiException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.", 404);
        }

        return repository;
    }

    /**
     * 암호화된 GitHub 토큰을 복호화합니다.
     */
    public String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            log.error("GitHub 토큰 복호화 실패: {}", e.getMessage());
            throw new GitHubTokenException("GitHub 토큰이 유효하지 않습니다.", e);
        }
    }

    /**
     * 현재 로그인한 사용자의 GitHub 사용자명을 조회합니다.
     */
    public String getUsernameFromToken(String token) {
        Map<String, Object> userInfo = callGitHubApi(
                webClient.get()
                        .uri(GitHubApiConstants.USER_INFO_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().bodyToMono(Map.class),
                "사용자 정보 조회"
        );

        return userInfo != null ? userInfo.get("login").toString() : "unknown";
    }

    /**
     * GitHub API 호출을 처리하고 오류를 적절히 처리합니다.
     */
    public <T> T callGitHubApi(reactor.core.publisher.Mono<T> apiCall, String apiName) {
        try {
            return apiCall.block();
        } catch (WebClientResponseException e) {
            handleWebClientException(e, apiName);
            return null;
        } catch (Exception e) {
            log.error("GitHub API 호출 중 오류 발생 ({}): {}", apiName, e.getMessage(), e);
            throw new GitHubApiException("GitHub API 호출 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * WebClient 예외를 처리합니다.
     */
    private void handleWebClientException(WebClientResponseException e, String apiName) {
        log.error("GitHub API 호출 실패 ({}): {} - {}", apiName, e.getStatusCode(), e.getMessage());

        int statusCode = e.getStatusCode().value();

        if (statusCode == 401) {
            throw new GitHubTokenException("GitHub 토큰이 유효하지 않습니다.");
        } else if (statusCode == 403) {
            throw new GitHubApiException("GitHub API 접근 권한이 없습니다.", 403);
        } else if (statusCode == 404) {
            throw new GitHubApiException("요청한 리소스를 찾을 수 없습니다.", 404);
        } else if (statusCode == 422) {
            throw new GitHubApiException("잘못된 GitHub API 요청입니다.", 422);
        } else if (statusCode == 429) {
            throw new GitHubApiException("GitHub API 호출 한도를 초과했습니다.", 429);
        } else if (e.getStatusCode().is4xxClientError()) {
            throw new GitHubApiException("잘못된 요청입니다: " + e.getMessage(), statusCode);
        } else {
            throw new GitHubApiException("GitHub 서버 오류가 발생했습니다.", statusCode);
        }
    }
}

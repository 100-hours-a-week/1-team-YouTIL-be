package com.youtil.Api.Github.Util;

import com.youtil.Common.Enums.TilMessageCode;
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
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
    }

    /**
     * 레포지토리 ID로 직접 레포지토리 정보를 조회합니다.
     * 조직/개인 레포지토리 구분 없이 사용자가 접근 가능한 레포지토리를 조회합니다.
     */
    public Map<String, Object> getRepositoryById(Long repositoryId, String token) {
        return callGitHubApi(
                webClient.get()
                        .uri(GitHubApiConstants.REPOSITORIES_BASE_URL, repositoryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map.class),
                "레포지토리 정보 조회"
        );
    }

    /**
     * 암호화된 GitHub 토큰을 복호화합니다.
     */
    public String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            log.error("GitHub 토큰 복호화 실패: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_INVALID.getMessage());
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
            return null; // 도달하지 않음 (예외 발생)
        } catch (Exception e) {
            log.error("GitHub API 호출 중 예상치 못한 오류 발생 ({}): {}", apiName, e.getMessage(), e);
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage());
        }
    }

    /**
     * WebClient 예외를 처리합니다.
     */
    private void handleWebClientException(WebClientResponseException e, String apiName) {
        log.error("GitHub API 호출 실패 ({}): {} - {}", apiName, e.getStatusCode(), e.getMessage());

        if (e.getStatusCode().is4xxClientError()) {
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_INVALID.getMessage());
            } else if (e.getStatusCode().value() == 403) {
                throw new RuntimeException(TilMessageCode.GITHUB_API_PERMISSION_DENIED.getMessage());
            } else if (e.getStatusCode().value() == 404) {
                throw new RuntimeException(TilMessageCode.GITHUB_RESOURCE_NOT_FOUND.getMessage());
            } else if (e.getStatusCode().value() == 422) {
                throw new RuntimeException(TilMessageCode.GITHUB_INVALID_REQUEST.getMessage() + ": " + e.getMessage());
            } else {
                throw new RuntimeException(TilMessageCode.GITHUB_INVALID_REQUEST.getMessage() + ": " + e.getStatusCode().value());
            }
        } else {
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage());
        }
    }
}

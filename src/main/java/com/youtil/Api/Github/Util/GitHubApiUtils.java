package com.youtil.Api.Github.Util;

import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
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

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    /**
     * 사용자의 GitHub 토큰이 있는지 확인합니다.
     */
    public void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
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
                        .uri(GITHUB_API_BASE_URL + "/user")
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

    /**
     * 조직 ID로부터 조직 로그인명을 조회합니다.
     */
    public String getOrganizationLogin(Long organizationId, String token) {
        Map<String, Object>[] orgs = callGitHubApi(
                webClient.get()
                        .uri(GITHUB_API_BASE_URL + "/user/orgs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map[].class),
                "사용자 조직 목록 조회"
        );

        if (orgs == null || orgs.length == 0) {
            throw new RuntimeException(TilMessageCode.GITHUB_USER_ORGS_NOT_FOUND.getMessage());
        }

        for (Map<String, Object> org : orgs) {
            if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                return org.get("login").toString();
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_ORG_NOT_FOUND.getMessage());
    }

    /**
     * 조직 내 레포지토리 ID로부터 레포지토리 이름을 조회합니다.
     */
    public String getRepositoryNameFromOrg(String owner, Long repositoryId, String token) {
        Map<String, Object>[] repos = callGitHubApi(
                webClient.get()
                        .uri(GITHUB_API_BASE_URL + "/orgs/" + owner + "/repos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map[].class),
                "조직 레포지토리 목록 조회 - " + owner
        );

        if (repos == null || repos.length == 0) {
            throw new RuntimeException(TilMessageCode.GITHUB_ORG_REPOS_NOT_FOUND.getMessage());
        }

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                return repo.get("name").toString();
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());
    }

    /**
     * 개인 레포지토리 ID로부터 소유자와 레포지토리 이름을 조회합니다.
     */
    public Map.Entry<String, String> getPersonalRepoInfo(Long repositoryId, String token) {
        Map<String, Object>[] repos = callGitHubApi(
                webClient.get()
                        .uri(GITHUB_API_BASE_URL + "/user/repos?affiliation=owner,collaborator")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map[].class),
                "사용자 레포지토리 목록 조회"
        );

        if (repos == null || repos.length == 0) {
            throw new RuntimeException(TilMessageCode.GITHUB_USER_REPOS_NOT_FOUND.getMessage());
        }

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                String repoName = repo.get("name").toString();
                String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
                return Map.entry(owner, repoName);
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());
    }
}
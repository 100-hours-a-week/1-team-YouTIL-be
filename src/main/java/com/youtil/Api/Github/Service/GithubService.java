package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Exception.GithubException.GitHubExceptions.*;
import com.youtil.Model.User;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Util.EntityValidator;
import com.youtil.Api.Github.Constants.GitHubApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.youtil.Api.Github.Constants.GithubCacheConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubService {

    private final WebClient webClient;
    private final EntityValidator entityValidator;
    private final GitHubCacheHelper cacheHelper;
    private final GitHubApiUtils gitHubApiUtils;

    /**
     * 사용자의 깃허브 조직 목록을 조회합니다. (캐싱 + 페이지네이션 적용)
     */
    public GithubResponseDTO.OrganizationResponseDTO getOrganizations(Long userId, int page, int offset) {
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildOrganizationsCacheKey(userId, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "organizations",
                GithubResponseDTO.OrganizationResponseDTO.class,
                () -> {
                    GithubResponseDTO.OrganizationResponseDTO response = fetchOrganizationsFromGithub(user, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, ORG_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 특정 조직의 레포지토리 목록을 조회합니다.
     */
    public GithubResponseDTO.RepositoryResponseDTO getRepositoriesByOrganizationId(
            Long userId, Long organizationId, int page, int offset) {
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildRepositoriesCacheKey(userId, organizationId, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "repositories",
                GithubResponseDTO.RepositoryResponseDTO.class,
                () -> {
                    GithubResponseDTO.RepositoryResponseDTO response = fetchRepositoriesFromGithub(user, organizationId, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, REPO_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 사용자의 개인 레포지토리 목록을 조회합니다.
     */
    public GithubResponseDTO.RepositoryResponseDTO getUserRepositories(Long userId, int page, int offset) {
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildUserRepositoriesCacheKey(userId, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "user_repositories",
                GithubResponseDTO.RepositoryResponseDTO.class,
                () -> {
                    GithubResponseDTO.RepositoryResponseDTO response = fetchUserRepositoriesFromGithub(user, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, REPO_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 조직 레포지토리의 브랜치 목록을 조회합니다.
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryId(
            Long userId, Long organizationId, Long repositoryId, int page, int offset) {

        if (repositoryId == null) {
            throw new NullPointerException("레포지토리 ID는 null일 수 없습니다.");
        }

        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildBranchesCacheKey(userId, repositoryId, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "branches",
                GithubResponseDTO.BranchResponseDTO.class,
                () -> {
                    GithubResponseDTO.BranchResponseDTO response = fetchBranchesFromGithub(user, organizationId, repositoryId, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, BRANCH_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 개인 레포지토리의 브랜치 목록을 조회합니다.
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryIdWithoutOrg(
            Long userId, Long repositoryId, int page, int offset) {

        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildBranchesCacheKey(userId, repositoryId, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "personal_branches",
                GithubResponseDTO.BranchResponseDTO.class,
                () -> {
                    GithubResponseDTO.BranchResponseDTO response = fetchPersonalBranchesFromGithub(user, repositoryId, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, BRANCH_CACHE_TTL);
                    return response;
                }
        );
    }

    // ============ 캐시 키 생성 메서드들 ============

    /**
     * 조직 목록 캐시 키 생성
     */
    private String buildOrganizationsCacheKey(Long userId, int page, int offset) {
        return String.format("%s%d:page:%d:offset:%d", ORG_CACHE_KEY, userId, page, offset);
    }

    /**
     * 조직 레포지토리 목록 캐시 키 생성
     */
    private String buildRepositoriesCacheKey(Long userId, Long organizationId, int page, int offset) {
        return String.format("%s%d:org:%d:page:%d:offset:%d", REPO_CACHE_KEY, userId, organizationId, page, offset);
    }

    /**
     * 개인 레포지토리 캐시 키 생성
     */
    private String buildUserRepositoriesCacheKey(Long userId, int page, int offset) {
        return String.format("%s%d:user:page:%d:offset:%d", REPO_CACHE_KEY, userId, page, offset);
    }

    /**
     * 브랜치 목록 캐시 키 생성
     */
    private String buildBranchesCacheKey(Long userId, Long repositoryId, int page, int offset) {
        return String.format("%s%d:repo:%d:page:%d:offset:%d", BRANCH_CACHE_KEY, userId, repositoryId, page, offset);
    }


    /**
     * GitHub API에서 조직 목록 조회
     */
    private GithubResponseDTO.OrganizationResponseDTO fetchOrganizationsFromGithub(User user, int page, int offset) {
        String accessToken = gitHubApiUtils.decryptToken(user.getGithubToken());

        try {
            log.info("GitHub 조직 목록 조회 - 사용자: {}, 프론트엔드 페이지: {} (0부터 시작), 사이즈: {}", user.getId(), page, offset);

            int githubApiPage = page + 1;

            Map<String, Object>[] organizationsResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GitHubApiConstants.USER_ORGS_URL)
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", offset)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 조직 API 응답: {}개 조직 조회됨 (GitHub API 페이지: {})",
                    organizationsResponse != null ? organizationsResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toOrganizationResponse(organizationsResponse, page, offset);
        } catch (WebClientResponseException e) {
            handleWebClientException(e, "조직 목록 조회");
            return null;
        } catch (Exception e) {
            log.error("조직 목록 조회 중 예상치 못한 오류 발생", e);
            throw new GitHubApiException("GitHub 조직 목록 조회 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 조직의 접근 가능한 레포지토리 조회
     */
    private GithubResponseDTO.RepositoryResponseDTO fetchRepositoriesFromGithub(
            User user, Long organizationId, int page, int offset) {
        log.info("접근 가능한 레포지토리 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}, 프론트엔드 페이지: {}, 항목수: {}",
                user.getId(), organizationId, page, offset);

        gitHubApiUtils.validateToken(user);
        String accessToken = gitHubApiUtils.decryptToken(user.getGithubToken());

        try {
            Set<Map<String, Object>> directRepos = fetchDirectCollaboratorRepos(accessToken, organizationId);
            List<Map<String, Object>> userTeams = fetchUserTeams(accessToken, organizationId);
            Set<Map<String, Object>> indirectRepos = fetchTeamAccessibleRepos(userTeams, accessToken, organizationId);
            Set<Map<String, Object>> allRepos = mergeWithoutDuplication(directRepos, indirectRepos);

            if (allRepos.isEmpty()) {
                log.info("접근 가능한 레포지토리가 없어 조직 전체 레포지토리 조회 (fallback)");
                Map<String, Object>[] fallbackRepos = handleGitHubApiCall(
                        webClient.get()
                                .uri("https://api.github.com/orgs/" + organizationId + "/repos?per_page=100")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .retrieve()
                                .bodyToMono(Map[].class),
                        "조직 전체 레포 목록 조회 (fallback)"
                );

                allRepos.addAll(Arrays.asList(fallbackRepos));
            }

            List<Map<String, Object>> repoList = new ArrayList<>(allRepos);
            repoList.sort((a, b) -> {
                String nameA = (String) a.get("name");
                String nameB = (String) b.get("name");
                return nameA.compareToIgnoreCase(nameB);
            });

            int totalRepos = repoList.size();
            int startIndex = page * offset;
            int endIndex = Math.min(startIndex + offset, totalRepos);

            if (startIndex >= totalRepos) {
                log.info("요청된 페이지가 범위를 벗어남: startIndex={}, totalRepos={}", startIndex, totalRepos);
                return GitHubDtoConverter.toRepositoryResponse(new Map[0], page, offset);
            }

            List<Map<String, Object>> pageRepos = repoList.subList(startIndex, endIndex);
            Map<String, Object>[] pageReposArray = pageRepos.toArray(new Map[0]);

            log.info("페이지네이션 적용 결과: 전체 {}개 중 {}개 반환 (페이지 {}, 사이즈 {})",
                    totalRepos, pageReposArray.length, page, offset);

            return GitHubDtoConverter.toRepositoryResponse(pageReposArray, page, offset);

        } catch (GitHubApiException | GitHubTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("레포지토리 목록 조회 중 예상치 못한 오류 발생", e);
            throw new GitHubApiException("GitHub 레포지토리 목록 조회 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 사용자 소유 레포지토리 조회
     */
    private GithubResponseDTO.RepositoryResponseDTO fetchUserRepositoriesFromGithub(User user, int page, int offset) {
        String accessToken = gitHubApiUtils.decryptToken(user.getGithubToken());

        try {
            log.info("개인 레포지토리 목록 조회 - 사용자: {}, 프론트엔드 페이지: {}, 사이즈: {}", user.getId(), page, offset);

            int githubApiPage = page + 1;

            Map<String, Object>[] repositoriesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GitHubApiConstants.USER_REPOS_URL)
                            .queryParam("affiliation", "owner")
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", offset)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 개인 레포지토리 API 응답: {}개 레포지토리 조회됨 (GitHub API 페이지: {})",
                    repositoriesResponse != null ? repositoriesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toRepositoryResponse(repositoriesResponse, page, offset);
        } catch (WebClientResponseException e) {
            handleWebClientException(e, "개인 레포지토리 조회");
            return null;
        } catch (Exception e) {
            throw new GitHubApiException("GitHub 사용자 레포지토리 목록 조회 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 조직 레포지토리 브랜치 조회
     */
    private GithubResponseDTO.BranchResponseDTO fetchBranchesFromGithub(
            User user, Long organizationId, Long repositoryId, int page, int offset) {
        String accessToken = gitHubApiUtils.decryptToken(user.getGithubToken());

        try {
            Map<String, Object> repoMetadata = handleGitHubApiCall(
                    webClient.get()
                            .uri(GitHubApiConstants.REPOSITORIES_BASE_URL + repositoryId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map.class),
                    "레포지토리 메타데이터 조회"
            );

            if (repoMetadata == null || !repoMetadata.containsKey("name") || !repoMetadata.containsKey("owner")) {
                throw new GitHubApiException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId, 404);
            }

            String repoName = repoMetadata.get("name").toString();
            String ownerLogin = ((Map<String, Object>) repoMetadata.get("owner")).get("login").toString();

            int githubApiPage = page + 1;

            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(GitHubApiConstants.REPOS_BASE_URL + ownerLogin + "/" + repoName + GitHubApiConstants.BRANCHES_PATH)
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", offset)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 브랜치 API 응답: {}개 브랜치 조회됨 (GitHub API 페이지: {})",
                    branchesResponse != null ? branchesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toBranchResponse(branchesResponse, page, offset);
        } catch (GitHubApiException | GitHubTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 개인 레포지토리 브랜치 조회 (페이지네이션 적용)
     */
    private GithubResponseDTO.BranchResponseDTO fetchPersonalBranchesFromGithub(
            User user, Long repositoryId, int page, int offset) {

        gitHubApiUtils.validateToken(user);
        String accessToken = gitHubApiUtils.decryptToken(user.getGithubToken());

        try {
            Map<String, Object> repoMetadata = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/repositories/" + repositoryId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map.class),
                    "개인 레포지토리 메타데이터 조회"
            );

            if (repoMetadata == null || !repoMetadata.containsKey("name") || !repoMetadata.containsKey("owner")) {
                throw new GitHubApiException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId, 404);
            }

            String repoName = repoMetadata.get("name").toString();
            String ownerLogin = ((Map<String, Object>) repoMetadata.get("owner")).get("login").toString();

            log.info("개인 레포지토리 브랜치 목록 조회 - 소유자: {}, 레포: {}, 프론트엔드 페이지: {}, 사이즈: {}",
                    ownerLogin, repoName, page, offset);

            int githubApiPage = page + 1;

            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + ownerLogin + "/" + repoName + "/branches")
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", offset)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 개인 레포지토리 브랜치 API 응답: {}개 브랜치 조회됨 (GitHub API 페이지: {})",
                    branchesResponse != null ? branchesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toBranchResponse(branchesResponse, page, offset);
        } catch (GitHubApiException | GitHubTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.", 500);
        }
    }

    // ============ 유틸리티 메서드들 ============

    /**
     * GitHub 토큰 검증
     */
    private Set<Map<String, Object>> fetchDirectCollaboratorRepos(String accessToken, Long organizationId) {
        Map<String, Object>[] result = handleGitHubApiCall(
                webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(GitHubApiConstants.USER_REPOS_URL)
                                .queryParam("affiliation", "owner,collaborator")
                                .queryParam("per_page", 100)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map[].class),
                "직접 콜라보레이터 레포 조회"
        );

        return Arrays.stream(result)
                .filter(repo -> isTargetOrganization(repo, organizationId))
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 소속된 팀 목록 조회
     */
    private List<Map<String, Object>> fetchUserTeams(String accessToken, Long organizationId) {
        Map<String, Object>[] result = handleGitHubApiCall(
                webClient.get()
                        .uri(GitHubApiConstants.USER_TEAMS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map[].class),
                "유저 팀 목록 조회"
        );

        return Arrays.stream(result)
                .filter(team -> {
                    Map<String, Object> org = (Map<String, Object>) team.get("organization");
                    return org != null && organizationId.equals(((Number) org.get("id")).longValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * 팀을 통해 접근 가능한 레포지토리 조회
     */
    private Set<Map<String, Object>> fetchTeamAccessibleRepos(List<Map<String, Object>> teams, String accessToken, Long organizationId) {
        Set<Map<String, Object>> repos = new HashSet<>();

        for (Map<String, Object> team : teams) {
            Number teamId = (Number) team.get("id");
            if (teamId == null) continue;

            Map<String, Object>[] teamRepos = handleGitHubApiCall(
                    webClient.get()
                            .uri(GitHubApiConstants.TEAMS_BASE_URL + teamId.longValue() + GitHubApiConstants.REPOS_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "팀 레포 조회"
            );

            for (Map<String, Object> repo : teamRepos) {
                if (isTargetOrganization(repo, organizationId)) {
                    repos.add(repo);
                }
            }
        }

        return repos;
    }

    /**
     * 중복 제거하여 레포지토리 목록 병합
     */
    private Set<Map<String, Object>> mergeWithoutDuplication(Set<Map<String, Object>> set1, Set<Map<String, Object>> set2) {
        Set<String> seen = new HashSet<>();
        Set<Map<String, Object>> merged = new HashSet<>();

        Stream.concat(set1.stream(), set2.stream())
                .filter(repo -> seen.add((String) repo.get("full_name")))
                .forEach(merged::add);

        return merged;
    }

    /**
     * 특정 조직의 레포지토리인지 확인
     */
    private boolean isTargetOrganization(Map<String, Object> repo, Long targetOrganizationId) {
        if (!repo.containsKey("owner")) return false;

        Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
        Object idObj = owner.get("id");

        if (idObj instanceof Integer) {
            return ((Integer) idObj).longValue() == targetOrganizationId;
        } else if (idObj instanceof Long) {
            return ((Long) idObj).equals(targetOrganizationId);
        }

        return false;
    }

    /**
     * GitHub API 호출 및 에러 처리
     */
    private <T> T handleGitHubApiCall(reactor.core.publisher.Mono<T> apiCall, String apiName) {
        try {
            return apiCall.block();
        } catch (WebClientResponseException e) {
            handleWebClientException(e, apiName);
            return null;
        } catch (Exception e) {
            throw new GitHubApiException("GitHub API 호출 중 오류가 발생했습니다: " + e.getMessage(), 500);
        }
    }

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

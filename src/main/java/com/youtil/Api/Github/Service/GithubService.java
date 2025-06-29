package com.youtil.Api.Github.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;
    private final GitHubCacheHelper cacheHelper;

    /**
     * 사용자의 깃허브 조직 목록을 조회합니다. (캐싱 + 페이지네이션 적용)
     */
    public GithubResponseDTO.OrganizationResponseDTO getOrganizations(Long userId, Integer page, Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String cacheKey = buildOrganizationsCacheKey(userId, page, size);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "organizations",
                GithubResponseDTO.OrganizationResponseDTO.class,
                () -> {
                    GithubResponseDTO.OrganizationResponseDTO response = fetchOrganizationsFromGithub(user, page, size);
                    cacheHelper.saveToCache(cacheKey, response, ORG_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 특정 조직의 레포지토리 목록을 조회합니다.
     */
    public GithubResponseDTO.RepositoryResponseDTO getRepositoriesByOrganizationId(
            Long userId, Long organizationId, Integer page, Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String cacheKey = buildRepositoriesCacheKey(userId, organizationId, page, size);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "repositories",
                GithubResponseDTO.RepositoryResponseDTO.class,
                () -> {
                    GithubResponseDTO.RepositoryResponseDTO response = fetchRepositoriesFromGithub(user, organizationId, page, size);
                    cacheHelper.saveToCache(cacheKey, response, REPO_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 사용자의 개인 레포지토리 목록을 조회합니다.
     */
    public GithubResponseDTO.RepositoryResponseDTO getUserRepositories(Long userId, Integer page, Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String cacheKey = buildUserRepositoriesCacheKey(userId, page, size);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "user_repositories",
                GithubResponseDTO.RepositoryResponseDTO.class,
                () -> {
                    GithubResponseDTO.RepositoryResponseDTO response = fetchUserRepositoriesFromGithub(user, page, size);
                    cacheHelper.saveToCache(cacheKey, response, REPO_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 조직 레포지토리의 브랜치 목록을 조회합니다.
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryId(
            Long userId, Long organizationId, Long repositoryId, Integer page, Integer size) {

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String cacheKey = buildBranchesCacheKey(userId, repositoryId, page, size);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "branches",
                GithubResponseDTO.BranchResponseDTO.class,
                () -> {
                    GithubResponseDTO.BranchResponseDTO response = fetchBranchesFromGithub(user, organizationId, repositoryId, page, size);
                    cacheHelper.saveToCache(cacheKey, response, BRANCH_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 개인 레포지토리의 브랜치 목록을 조회합니다.
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryIdWithoutOrg(
            Long userId, Long repositoryId, Integer page, Integer size) {

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String cacheKey = buildBranchesCacheKey(userId, repositoryId, page, size);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "personal_branches",
                GithubResponseDTO.BranchResponseDTO.class,
                () -> {
                    GithubResponseDTO.BranchResponseDTO response = fetchPersonalBranchesFromGithub(user, repositoryId, page, size);
                    cacheHelper.saveToCache(cacheKey, response, BRANCH_CACHE_TTL);
                    return response;
                }
        );
    }


    // ============ 캐시 키 생성 메서드들 ============

    /**
     * 조직 목록 캐시 키 생성
     */
    private String buildOrganizationsCacheKey(Long userId, Integer page, Integer size) {
        return String.format("%s%d:page:%d:size:%d", ORG_CACHE_KEY, userId, page, size);
    }

    /**
     * 조직 레포지토리 목록 캐시 키 생성
     */
    private String buildRepositoriesCacheKey(Long userId, Long organizationId, Integer page, Integer size) {
        return String.format("%s%d:org:%d:page:%d:size:%d", REPO_CACHE_KEY, userId, organizationId, page, size);
    }

    /**
     * 개인 레포지토리 캐시 키 생성
     */
    private String buildUserRepositoriesCacheKey(Long userId, Integer page, Integer size) {
        return String.format("%s%d:user:page:%d:size:%d", REPO_CACHE_KEY, userId, page, size);
    }

    /**
     * 브랜치 목록 캐시 키 생성
     */
    private String buildBranchesCacheKey(Long userId, Long repositoryId, Integer page, Integer size) {
        return String.format("%s%d:repo:%d:page:%d:size:%d", BRANCH_CACHE_KEY, userId, repositoryId, page, size);
    }


    /**
     * GitHub API에서 조직 목록 조회
     */
    private GithubResponseDTO.OrganizationResponseDTO fetchOrganizationsFromGithub(User user, Integer page, Integer size) {
        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_DECRYPT_ERROR.getMessage());
        }

        try {
            log.info("GitHub 조직 목록 조회 - 사용자: {}, 프론트엔드 페이지: {} (0부터 시작), 사이즈: {}", user.getId(), page, size);

            // GitHub API는 1부터 시작하므로 +1 해서 전달
            int githubApiPage = page + 1;

            Map<String, Object>[] organizationsResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/user/orgs")
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 조직 API 응답: {}개 조직 조회됨 (GitHub API 페이지: {})",
                    organizationsResponse != null ? organizationsResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toOrganizationResponse(organizationsResponse, page, size);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("조직 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage());
        }
    }

    /**
     * 조직의 접근 가능한 레포지토리 조회
     */
    private GithubResponseDTO.RepositoryResponseDTO fetchRepositoriesFromGithub(
            User user, Long organizationId, Integer page, Integer size) {
        log.info("접근 가능한 레포지토리 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}, 프론트엔드 페이지: {}, 항목수: {}",
                user.getId(), organizationId, page, size);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // 1. 직접 콜라보레이터로 참여한 레포지토리 조회
            Set<Map<String, Object>> directRepos = fetchDirectCollaboratorRepos(accessToken, organizationId);
            log.info("직접 콜라보레이터 레포지토리: {}개", directRepos.size());

            // 2. 유저가 소속된 팀 목록 조회
            List<Map<String, Object>> userTeams = fetchUserTeams(accessToken, organizationId);
            log.info("사용자 소속 팀: {}개", userTeams.size());

            // 3. 각 팀이 접근 가능한 레포지토리 조회
            Set<Map<String, Object>> indirectRepos = fetchTeamAccessibleRepos(userTeams, accessToken, organizationId);
            log.info("팀 기반 접근 가능 레포지토리: {}개", indirectRepos.size());

            // 4. 직접 + 간접 레포 병합 (중복 제거)
            Set<Map<String, Object>> allRepos = mergeWithoutDuplication(directRepos, indirectRepos);
            log.info("병합 후 총 레포지토리: {}개", allRepos.size());

            // 5. fallback: 직접/간접 레포가 하나도 없을 경우, 조직 전체 레포 조회
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
                log.info("fallback 후 총 레포지토리: {}개", allRepos.size());
            }

            // 6. 페이지네이션 적용을 위해 List로 변환 후 정렬
            List<Map<String, Object>> repoList = new ArrayList<>(allRepos);
            repoList.sort((a, b) -> {
                String nameA = (String) a.get("name");
                String nameB = (String) b.get("name");
                return nameA.compareToIgnoreCase(nameB);
            });

            // 7. 수동 페이지네이션 적용
            int totalRepos = repoList.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalRepos);

            // 페이지 범위 검증
            if (startIndex >= totalRepos) {
                log.info("요청된 페이지가 범위를 벗어남: startIndex={}, totalRepos={}", startIndex, totalRepos);
                return GitHubDtoConverter.toRepositoryResponse(new Map[0], page, size);
            }

            // 해당 페이지의 레포지토리만 추출
            List<Map<String, Object>> pageRepos = repoList.subList(startIndex, endIndex);
            Map<String, Object>[] pageReposArray = pageRepos.toArray(new Map[0]);

            log.info("페이지네이션 적용 결과: 전체 {}개 중 {}개 반환 (페이지 {}, 사이즈 {})",
                    totalRepos, pageReposArray.length, page, size);

            return GitHubDtoConverter.toRepositoryResponse(pageReposArray, page, size);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("레포지토리 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("GitHub 레포지토리 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 소유 레포지토리 조회
     */
    private GithubResponseDTO.RepositoryResponseDTO fetchUserRepositoriesFromGithub(User user, Integer page, Integer size) {
        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            log.info("개인 레포지토리 목록 조회 - 사용자: {}, 프론트엔드 페이지: {}, 사이즈: {}", user.getId(), page, size);

            // GitHub API는 1부터 시작하므로 +1 해서 전달
            int githubApiPage = page + 1;

            Map<String, Object>[] repositoriesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/user/repos")
                            .queryParam("affiliation", "owner")
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 개인 레포지토리 API 응답: {}개 레포지토리 조회됨 (GitHub API 페이지: {})",
                    repositoriesResponse != null ? repositoriesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toRepositoryResponse(repositoriesResponse, page, size);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 사용자 레포지토리 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 조직 레포지토리 브랜치 조회
     */
    private GithubResponseDTO.BranchResponseDTO fetchBranchesFromGithub(
            User user, Long organizationId, Long repositoryId, Integer page, Integer size) {
        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // repositoryId를 기반으로 레포지토리 메타데이터 조회
            Map<String, Object> repoMetadata = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/repositories/" + repositoryId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map.class),
                    "레포지토리 메타데이터 조회"
            );

            if (repoMetadata == null || !repoMetadata.containsKey("name") || !repoMetadata.containsKey("owner")) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            String repoName = repoMetadata.get("name").toString();
            String ownerLogin = ((Map<String, Object>) repoMetadata.get("owner")).get("login").toString();

            // GitHub API는 1부터 시작하므로 +1 해서 전달
            int githubApiPage = page + 1;

            // 브랜치 목록 조회 (페이지네이션 적용)
            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + ownerLogin + "/" + repoName + "/branches")
                            .queryParam("page", githubApiPage)  // GitHub API용으로 +1
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 브랜치 API 응답: {}개 브랜치 조회됨 (GitHub API 페이지: {})",
                    branchesResponse != null ? branchesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toBranchResponse(branchesResponse, page, size);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 개인 레포지토리 브랜치 조회 (페이지네이션 적용)
     */
    private GithubResponseDTO.BranchResponseDTO fetchPersonalBranchesFromGithub(
            User user, Long repositoryId, Integer page, Integer size) {

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // repositoryId를 기반으로 레포지토리 메타데이터 조회
            Map<String, Object> repoMetadata = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/repositories/" + repositoryId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map.class),
                    "개인 레포지토리 메타데이터 조회"
            );

            if (repoMetadata == null || !repoMetadata.containsKey("name") || !repoMetadata.containsKey("owner")) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            String repoName = repoMetadata.get("name").toString();
            String ownerLogin = ((Map<String, Object>) repoMetadata.get("owner")).get("login").toString();

            log.info("개인 레포지토리 브랜치 목록 조회 - 소유자: {}, 레포: {}, 프론트엔드 페이지: {}, 사이즈: {}",
                    ownerLogin, repoName, page, size);

            // GitHub API는 1부터 시작하므로 +1 해서 전달
            int githubApiPage = page + 1;

            // 브랜치 목록 조회 (페이지네이션 적용)
            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + ownerLogin + "/" + repoName + "/branches")
                            .queryParam("page", githubApiPage)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 개인 레포지토리 브랜치 API 응답: {}개 브랜치 조회됨 (GitHub API 페이지: {})",
                    branchesResponse != null ? branchesResponse.length : 0, githubApiPage);

            return GitHubDtoConverter.toBranchResponse(branchesResponse, page, size);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    // ============ 유틸리티 메서드들 ============

    /**
     * GitHub 토큰 검증
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
    }

    private Set<Map<String, Object>> fetchDirectCollaboratorRepos(String accessToken, Long organizationId) {
        Map<String, Object>[] result = handleGitHubApiCall(
                webClient.get()
                        .uri("https://api.github.com/user/repos?affiliation=owner,collaborator&per_page=100")
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
                        .uri("https://api.github.com/user/teams")
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
                            .uri("https://api.github.com/teams/" + teamId.longValue() + "/repos")
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
            if (e.getStatusCode().is4xxClientError()) {
                if (e.getStatusCode().value() == 401) {
                    throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_INVALID.getMessage());
                } else if (e.getStatusCode().value() == 403) {
                    throw new RuntimeException(TilMessageCode.GITHUB_API_PERMISSION_DENIED.getMessage());
                } else if (e.getStatusCode().value() == 404) {
                    throw new RuntimeException(TilMessageCode.GITHUB_RESOURCE_NOT_FOUND.getMessage());
                } else {
                    throw new RuntimeException(TilMessageCode.GITHUB_INVALID_REQUEST.getMessage() + ": " + e.getStatusCode().value());
                }
            } else {
                throw new RuntimeException(TilMessageCode.GITHUB_SERVER_ERROR.getMessage() + ": " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }
    }
}

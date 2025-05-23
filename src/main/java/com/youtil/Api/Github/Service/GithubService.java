package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    /**
     * 사용자의 깃허브 조직 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param page   페이지 번호 (1부터 시작)
     * @param size   페이지당 항목 수
     * @return 깃허브 조직 목록
     */
    public GithubResponseDTO.OrganizationResponseDTO getOrganizations(Long userId, Integer page,
                                                                      Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_DECRYPT_ERROR.getMessage());
        }

        try {
            // GitHub API를 통해 사용자의 조직 목록 조회 (페이지네이션 적용)
            Map<String, Object>[] organizationsResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/user/orgs")
                            .queryParam("page", page)
                            .queryParam("per_page", size)  // GitHub API는 per_page 파라미터 사용
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            // GitHubDtoConverter 활용하여 DTO 변환
            return GitHubDtoConverter.toOrganizationResponse(organizationsResponse);
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("조직 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage());
        }
    }

    /**
     * 특정 조직의 레포지토리 목록을 조회합니다. (직접 콜라보레이터 + 팀 접근 권한 포함)
     *
     * @param userId         사용자 ID
     * @param organizationId 조직 ID
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getRepositoriesByOrganizationId(Long userId,
            Long organizationId) {
        log.info("접근 가능한 레포지토리 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}", userId, organizationId);

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        // 1. 직접 콜라보레이터로 참여한 레포지토리 조회
        Set<Map<String, Object>> directRepos = fetchDirectCollaboratorRepos(accessToken,
                organizationId);

        // 2. 유저가 소속된 팀 목록 조회
        List<Map<String, Object>> userTeams = fetchUserTeams(accessToken, organizationId);

        // 3. 각 팀이 접근 가능한 레포지토리 조회
        Set<Map<String, Object>> indirectRepos = fetchTeamAccessibleRepos(userTeams, accessToken,
                organizationId);

        // 4. 직접 + 간접 레포 병합 (중복 제거)
        Set<Map<String, Object>> allRepos = mergeWithoutDuplication(directRepos, indirectRepos);

        // 5. fallback: 직접/간접 레포가 하나도 없을 경우, 조직 전체 레포 조회
        if (allRepos.isEmpty()) {

            // 조직 전체 레포 목록 조회
            Map<String, Object>[] fallbackRepos = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/orgs/" + organizationId
                                    + "/repos?per_page=100")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "조직 전체 레포 목록 조회 (fallback)"
            );

            allRepos.addAll(Arrays.asList(fallbackRepos));
        }

        return GitHubDtoConverter.toRepositoryResponse(allRepos.toArray(new Map[0]));
    }


    /**
     * 특정 조직의 레포지토리 목록을 페이지네이션하여 조회합니다.
     *
     * @param userId         사용자 ID
     * @param organizationId 조직 ID
     * @param page           페이지 번호 (1부터 시작)
     * @param size           페이지당 항목 수
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getRepositoriesByOrganizationId(
            Long userId, Long organizationId, Integer page, Integer size) {
        log.info("레포지토리 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}, 페이지: {}, 항목수: {}",
                userId, organizationId, page, size);

        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // 조직 이름 조회
            Map<String, Object>[] organizations = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/user/orgs")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "사용자 조직 목록 조회"
            );

            final String organizationName;
            if (organizations != null) {
                String tempName = "";
                for (Map<String, Object> org : organizations) {
                    if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                        tempName = org.get("login").toString();
                        break;
                    }
                }
                organizationName = tempName;
            } else {
                organizationName = "";
            }

            if (organizationName.isEmpty()) {
                throw new RuntimeException("해당 ID의 조직을 찾을 수 없습니다: " + organizationId);
            }

            // 해당 조직의 레포지토리 목록 조회 (페이지네이션 적용)
            final String orgName = organizationName; // 람다에서 사용하기 위한 final 변수
            Map<String, Object>[] repositoriesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/orgs/" + orgName + "/repos")
                            .queryParam("page", page)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            // DTO 변환 및 응답 구성
            return GitHubDtoConverter.toRepositoryResponse(repositoriesResponse);
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("레포지토리 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("GitHub 레포지토리 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 레포지토리의 브랜치 목록을 조회합니다.
     *
     * @param userId         사용자 ID
     * @param organizationId 조직 ID
     * @param repositoryId   레포지토리 ID
     * @param page           페이지 번호 (1부터 시작)
     * @param size           페이지당 항목 수
     * @return 브랜치 목록
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryId(
            Long userId, Long organizationId, Long repositoryId, Integer page, Integer size) {

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

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

            if (repoMetadata == null || !repoMetadata.containsKey("name")
                    || !repoMetadata.containsKey("owner")) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            String repoName = repoMetadata.get("name").toString();
            String ownerLogin = ((Map<String, Object>) repoMetadata.get("owner")).get("login")
                    .toString();

            // 브랜치 목록 조회 (페이지네이션 적용)
            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + ownerLogin + "/" + repoName + "/branches")
                            .queryParam("page", page)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            return GitHubDtoConverter.toBranchResponse(branchesResponse);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 개인 레포지토리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param page   페이지 번호 (1부터 시작)
     * @param size   페이지당 항목 수
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getUserRepositories(Long userId, Integer page,
            Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // GitHub API를 통해 사용자의 레포지토리 목록 조회 (페이지네이션 적용)
            Map<String, Object>[] repositoriesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/user/repos")
                            .queryParam("affiliation", "owner")  // owner 권한이 있는 레포지토리만 조회
                            .queryParam("page", page)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            // DTO 변환 및 응답 구성
            return GitHubDtoConverter.toRepositoryResponse(repositoriesResponse);
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 사용자 레포지토리 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 개인 레포지토리의 브랜치 목록을 조회합니다.
     *
     * @param userId       사용자 ID
     * @param repositoryId 레포지토리 ID
     * @param page         페이지 번호 (1부터 시작)
     * @param size         페이지당 항목 수
     * @return 브랜치 목록
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryIdWithoutOrg(
            Long userId, Long repositoryId, Integer page, Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // 사용자 레포지토리 목록 조회
            Map<String, Object>[] repositories = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/user/repos?affiliation=owner")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "사용자 레포지토리 목록 조회"
            );

            // 레포지토리 정보 찾기
            final String repositoryName;
            final String ownerName;

            if (repositories != null) {
                String tempRepoName = "";
                String tempOwnerName = "";

                for (Map<String, Object> repo : repositories) {
                    if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                        tempRepoName = repo.get("name").toString();
                        Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
                        tempOwnerName = owner.get("login").toString();
                        break;
                    }
                }

                repositoryName = tempRepoName;
                ownerName = tempOwnerName;
            } else {
                repositoryName = "";
                ownerName = "";
            }

            if (repositoryName.isEmpty() || ownerName.isEmpty()) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            // 브랜치 목록 조회 (페이지네이션 적용)
            final String repoOwner = ownerName; // 람다에서 사용하기 위한 final 변수
            final String repoName = repositoryName; // 람다에서 사용하기 위한 final 변수

            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + repoOwner + "/" + repoName + "/branches")
                            .queryParam("page", page)
                            .queryParam("per_page", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            // DTO 변환 및 응답 구성
            return GitHubDtoConverter.toBranchResponse(branchesResponse);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 직접 콜라보레이터로 참여한 레포지토리를 조회합니다.
     */
    private Set<Map<String, Object>> fetchDirectCollaboratorRepos(String accessToken,
            Long organizationId) {
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
     * 사용자가 소속된 팀 목록을 조회합니다.
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
                    return org != null && organizationId.equals(
                            ((Number) org.get("id")).longValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * 각 팀이 접근 가능한 레포지토리를 조회합니다.
     */
    private Set<Map<String, Object>> fetchTeamAccessibleRepos(List<Map<String, Object>> teams,
            String accessToken, Long organizationId) {
        Set<Map<String, Object>> repos = new HashSet<>();

        for (Map<String, Object> team : teams) {
            Number teamId = (Number) team.get("id");
            if (teamId == null) {
                continue;
            }

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
     * 두 레포지토리 집합을 중복 없이 병합합니다.
     */
    private Set<Map<String, Object>> mergeWithoutDuplication(Set<Map<String, Object>> set1,
            Set<Map<String, Object>> set2) {
        Set<String> seen = new HashSet<>();
        Set<Map<String, Object>> merged = new HashSet<>();

        Stream.concat(set1.stream(), set2.stream())
                .filter(repo -> seen.add((String) repo.get("full_name")))
                .forEach(merged::add);

        return merged;
    }

    /**
     * 특정 레포지토리가 대상 조직에 속하는지 확인합니다.
     */
    private boolean isTargetOrganization(Map<String, Object> repo, Long targetOrganizationId) {
        if (!repo.containsKey("owner")) {
            return false;
        }

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
     * 사용자의 GitHub 토큰 유효성을 검사합니다.
     *
     * @param user 사용자 정보
     * @throws RuntimeException 토큰이 없거나 유효하지 않은 경우
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
    }

    /**
     * GitHub API 호출을 처리하고 오류를 적절히 처리합니다.
     *
     * @param <T>     응답 타입
     * @param apiCall API 호출 Mono
     * @param apiName API 호출 설명(로그용)
     * @return API 응답
     * @throws RuntimeException API 호출 실패 시
     */
    private <T> T handleGitHubApiCall(Mono<T> apiCall, String apiName) {
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

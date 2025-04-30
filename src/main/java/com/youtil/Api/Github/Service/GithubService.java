package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Dto.GithubResponseDTO.BranchItem;
import com.youtil.Api.Github.Dto.GithubResponseDTO.OrganizationItem;
import com.youtil.Api.Github.Dto.GithubResponseDTO.RepositoryItem;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * @return 깃허브 조직 목록
     */
    public GithubResponseDTO.OrganizationResponseDTO getOrganizations(Long userId) {
        log.info("조직 목록 조회 시작 - 사용자 ID: {}", userId);

        User user = entityValidator.getValidUserOrThrow(userId);
        log.info("사용자 정보 조회 성공 - 깃허브 토큰 존재 여부: {}", (user.getGithubToken() != null && !user.getGithubToken().isEmpty()));

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
            // 보안을 위해 전체 토큰을 로그에 출력하지 않고 앞부분만 출력
            log.info("토큰 복호화 성공 - 토큰 앞부분: {}", accessToken.substring(0, Math.min(10, accessToken.length())));
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // GitHub API를 통해 사용자의 조직 목록 조회
            Map<String, Object>[] organizationsResponse = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/user/orgs")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "사용자 조직 목록 조회"
            );

            log.info("GitHub API 응답 성공 - 조직 수: {}", organizationsResponse != null ? organizationsResponse.length : 0);

            List<OrganizationItem> organizations = new ArrayList<>();
            if (organizationsResponse != null) {
                organizations = Arrays.stream(organizationsResponse)
                        .map(org -> {
                            log.debug("조직 정보: {}", org);
                            return new OrganizationItem(
                                    Long.valueOf(org.get("id").toString()),
                                    org.get("login").toString());
                        })
                        .collect(Collectors.toList());
            }

            return GithubResponseDTO.OrganizationResponseDTO.builder()
                    .organizations(organizations)
                    .build();
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("조직 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("GitHub 조직 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 조직의 레포지토리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param organizationId 조직 ID
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getRepositoriesByOrganizationId(Long userId, Long organizationId) {
        log.info("레포지토리 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}", userId, organizationId);

        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
            log.info("토큰 복호화 성공");
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
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

            String organizationName = "";
            if (organizations != null) {
                for (Map<String, Object> org : organizations) {
                    if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                        organizationName = org.get("login").toString();
                        break;
                    }
                }
            }

            if (organizationName.isEmpty()) {
                log.warn("조직을 찾을 수 없음 - 조직 ID: {}", organizationId);
                throw new RuntimeException("해당 ID의 조직을 찾을 수 없습니다: " + organizationId);
            }

            log.info("조직 이름 조회 성공: {}", organizationName);

            // 해당 조직의 레포지토리 목록 조회
            Map<String, Object>[] repositoriesResponse = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/orgs/" + organizationName + "/repos")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "조직 레포지토리 목록 조회 - " + organizationName
            );

            log.info("레포지토리 목록 조회 성공 - 레포지토리 수: {}",
                    repositoriesResponse != null ? repositoriesResponse.length : 0);

            List<RepositoryItem> repositories = new ArrayList<>();
            if (repositoriesResponse != null) {
                repositories = Arrays.stream(repositoriesResponse)
                        .map(repo -> new RepositoryItem(
                                Long.valueOf(repo.get("id").toString()),
                                repo.get("name").toString()))
                        .collect(Collectors.toList());
            }

            return GithubResponseDTO.RepositoryResponseDTO.builder()
                    .repositories(repositories)
                    .build();
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
     * @param userId 사용자 ID
     * @param organizationId 조직 ID
     * @param repositoryId 레포지토리 ID
     * @return 브랜치 목록
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryId(Long userId, Long organizationId, Long repositoryId) {
        log.info("브랜치 목록 조회 시작 - 사용자 ID: {}, 조직 ID: {}, 레포지토리 ID: {}",
                userId, organizationId, repositoryId);

        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
            log.info("토큰 복호화 성공");
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
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

            String organizationName = "";
            if (organizations != null) {
                for (Map<String, Object> org : organizations) {
                    if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                        organizationName = org.get("login").toString();
                        break;
                    }
                }
            }

            if (organizationName.isEmpty()) {
                log.warn("조직을 찾을 수 없음 - 조직 ID: {}", organizationId);
                throw new RuntimeException("해당 ID의 조직을 찾을 수 없습니다: " + organizationId);
            }

            log.info("조직 이름 조회 성공: {}", organizationName);

            // 레포지토리 이름 조회
            Map<String, Object>[] repositories = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/orgs/" + organizationName + "/repos")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "조직 레포지토리 목록 조회 - " + organizationName
            );

            String repositoryName = "";
            if (repositories != null) {
                for (Map<String, Object> repo : repositories) {
                    if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                        repositoryName = repo.get("name").toString();
                        break;
                    }
                }
            }

            if (repositoryName.isEmpty()) {
                log.warn("레포지토리를 찾을 수 없음 - 레포지토리 ID: {}", repositoryId);
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            log.info("레포지토리 이름 조회 성공: {}", repositoryName);

            // 브랜치 목록 조회
            Map<String, Object>[] branchesResponse = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/repos/" + organizationName + "/" + repositoryName + "/branches")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "레포지토리 브랜치 목록 조회 - " + organizationName + "/" + repositoryName
            );

            log.info("브랜치 목록 조회 성공 - 브랜치 수: {}",
                    branchesResponse != null ? branchesResponse.length : 0);

            List<BranchItem> branches = new ArrayList<>();
            if (branchesResponse != null) {
                branches = Arrays.stream(branchesResponse)
                        .map(branch -> new BranchItem(branch.get("name").toString()))
                        .collect(Collectors.toList());
            }

            return GithubResponseDTO.BranchResponseDTO.builder()
                    .branches(branches)
                    .build();
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("브랜치 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 개인 레포지토리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getUserRepositories(Long userId) {
        log.info("사용자 개인 레포지토리 목록 조회 시작 - 사용자 ID: {}", userId);

        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
            log.info("토큰 복호화 성공");
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
        }

        try {
            // GitHub API를 통해 사용자의 레포지토리 목록 조회
            Map<String, Object>[] repositoriesResponse = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/user/repos?affiliation=owner")  // owner 권한이 있는 레포지토리만 조회
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "사용자 개인 레포지토리 목록 조회"
            );

            log.info("사용자 레포지토리 목록 조회 성공 - 레포지토리 수: {}",
                    repositoriesResponse != null ? repositoriesResponse.length : 0);

            List<RepositoryItem> repositories = new ArrayList<>();
            if (repositoriesResponse != null) {
                repositories = Arrays.stream(repositoriesResponse)
                        .map(repo -> new RepositoryItem(
                                Long.valueOf(repo.get("id").toString()),
                                repo.get("name").toString()))
                        .collect(Collectors.toList());
            }

            return GithubResponseDTO.RepositoryResponseDTO.builder()
                    .repositories(repositories)
                    .build();
        } catch (RuntimeException e) {
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("사용자 레포지토리 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("GitHub 사용자 레포지토리 목록 조회 중 오류가 발생했습니다.");
        }
    }

    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryIdWithoutOrg(Long userId, Long repositoryId) {
        log.info("개인 레포지토리 브랜치 목록 조회 - 사용자 ID: {}, 레포지토리 ID: {}", userId, repositoryId);

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
            log.info("토큰 복호화 성공");
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
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

            String repositoryName = "";
            String ownerName = ""; // 개인 깃허브 ID

            if (repositories != null) {
                for (Map<String, Object> repo : repositories) {
                    if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                        repositoryName = repo.get("name").toString();
                        Map<String, Object> owner = (Map<String, Object>) repo.get("owner");
                        ownerName = owner.get("login").toString();
                        break;
                    }
                }
            }

            if (repositoryName.isEmpty() || ownerName.isEmpty()) {
                log.warn("레포지토리를 찾을 수 없음 - 레포지토리 ID: {}", repositoryId);
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            log.info("레포지토리 정보 조회 성공: {}/{}", ownerName, repositoryName);

            // 브랜치 목록 조회
            Map<String, Object>[] branchesResponse = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/repos/" + ownerName + "/" + repositoryName + "/branches")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "개인 레포지토리 브랜치 목록 조회 - " + ownerName + "/" + repositoryName
            );

            List<GithubResponseDTO.BranchItem> branches = new ArrayList<>();
            if (branchesResponse != null) {
                branches = Arrays.stream(branchesResponse)
                        .map(branch -> new GithubResponseDTO.BranchItem(branch.get("name").toString()))
                        .collect(Collectors.toList());
            }

            return GithubResponseDTO.BranchResponseDTO.builder()
                    .branches(branches)
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("개인 레포지토리 브랜치 목록 조회 중 오류 발생", e);
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }


    /**
     * 사용자의 GitHub 토큰 유효성을 검사합니다.
     *
     * @param user 사용자 정보
     * @throws RuntimeException 토큰이 없거나 유효하지 않은 경우
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            log.warn("GitHub 토큰이 없음 - 사용자 ID: {}", user.getId());
            throw new RuntimeException("GitHub 토큰이 없습니다. 다시 로그인해주세요.");
        }
    }

    /**
     * GitHub API 호출을 처리하고 오류를 적절히 처리합니다.
     *
     * @param <T> 응답 타입
     * @param apiCall API 호출 Mono
     * @param apiName API 호출 설명(로그용)
     * @return API 응답
     * @throws RuntimeException API 호출 실패 시
     */
    private <T> T handleGitHubApiCall(Mono<T> apiCall, String apiName) {
        try {
            return apiCall.block();
        } catch (WebClientResponseException e) {
            log.error("GitHub API 오류: {}, 상태 코드: {}, 응답: {}",
                    apiName, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().is4xxClientError()) {
                if (e.getStatusCode().value() == 401) {
                    throw new RuntimeException("GitHub 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
                } else if (e.getStatusCode().value() == 403) {
                    throw new RuntimeException("GitHub API 호출 횟수 제한에 도달했거나 접근 권한이 없습니다.");
                } else if (e.getStatusCode().value() == 404) {
                    throw new RuntimeException("요청한 GitHub 리소스를 찾을 수 없습니다.");
                } else {
                    throw new RuntimeException("GitHub API 요청 오류: " + e.getStatusCode().value());
                }
            } else {
                throw new RuntimeException("GitHub 서버 오류: " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            log.error("GitHub API 호출 중 예상치 못한 오류: {}", apiName, e);
            throw new RuntimeException("GitHub API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


}
package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.GithubResponseDTO;
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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
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
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 항목 수
     * @return 깃허브 조직 목록
     */
    public GithubResponseDTO.OrganizationResponseDTO getOrganizations(Long userId, Integer page, Integer size) {
        User user = entityValidator.getValidUserOrThrow(userId);

        // 토큰 유효성 검사
        validateToken(user);

        String accessToken;
        try {
            accessToken = tokenEncryptor.decrypt(user.getGithubToken());
        } catch (Exception e) {
            log.error("토큰 복호화 오류", e);
            throw new RuntimeException("GitHub 토큰이 올바르지 않습니다. 다시 로그인해주세요.");
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
            throw new RuntimeException("GitHub 조직 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 조직의 레포지토리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param organizationId 조직 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 항목 수
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
     * @param userId 사용자 ID
     * @param organizationId 조직 ID
     * @param repositoryId 레포지토리 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 항목 수
     * @return 브랜치 목록
     */
    public GithubResponseDTO.BranchResponseDTO getBranchesByRepositoryId(
            Long userId, Long organizationId, Long repositoryId, Integer page, Integer size) {
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

            // 레포지토리 이름 조회
            final String orgName = organizationName; // 람다에서 사용하기 위한 final 변수
            Map<String, Object>[] repositories = handleGitHubApiCall(
                    webClient.get()
                            .uri("https://api.github.com/orgs/" + orgName + "/repos")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map[].class),
                    "조직 레포지토리 목록 조회 - " + orgName
            );

            final String repositoryName;
            if (repositories != null) {
                String tempName = "";
                for (Map<String, Object> repo : repositories) {
                    if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                        tempName = repo.get("name").toString();
                        break;
                    }
                }
                repositoryName = tempName;
            } else {
                repositoryName = "";
            }

            if (repositoryName.isEmpty()) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + repositoryId);
            }

            // 브랜치 목록 조회 (페이지네이션 적용)
            final String repoName = repositoryName; // 람다에서 사용하기 위한 final 변수
            Map<String, Object>[] branchesResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .path("/repos/" + orgName + "/" + repoName + "/branches")
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
            // 자체 정의한 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GitHub 브랜치 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 개인 레포지토리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 항목 수
     * @return 레포지토리 목록
     */
    public GithubResponseDTO.RepositoryResponseDTO getUserRepositories(Long userId, Integer page, Integer size) {
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
     * @param userId 사용자 ID
     * @param repositoryId 레포지토리 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 항목 수
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
     * 사용자의 GitHub 토큰 유효성을 검사합니다.
     *
     * @param user 사용자 정보
     * @throws RuntimeException 토큰이 없거나 유효하지 않은 경우
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
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
            throw new RuntimeException("GitHub API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
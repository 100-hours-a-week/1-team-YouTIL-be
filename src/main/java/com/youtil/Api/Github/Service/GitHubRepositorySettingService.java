package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GitHubRepositorySettingDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubRepositoryConfigUtil;
import com.youtil.Exception.GithubException.GitHubExceptions.*;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubRepositorySettingService {

    private final UserRepository userRepository;
    private final EntityValidator entityValidator;
    private final GitHubApiUtils gitHubApiUtils;

    /**
     * 기본 업로드 레포지토리 설정
     */
    @Transactional
    public GitHubRepositorySettingDTO.RepositorySettingResponse setDefaultRepository(
            GitHubRepositorySettingDTO.SetRepositoryRequest request, Long userId) {

        log.info("기본 레포지토리 설정 시작 - 사용자 ID: {}, 레포지토리 ID: {}",
                userId, request.getRepositoryId());

        // 사용자 조회 및 토큰 검증
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);
        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(request.getRepositoryId(), token);
        if (repoInfo == null || !repoInfo.containsKey("name") || !repoInfo.containsKey("owner")) {
            throw new RuntimeException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.");
        }

        String repoName = repoInfo.get("name").toString();
        String owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();

        log.info("레포지토리 정보 확인 - 소유자: {}, 레포명: {}", owner, repoName);

        String configString;
        if (request.getOrganizationId() != null) {
            // 조직 레포지토리
            configString = GitHubRepositoryConfigUtil.createOrgRepoConfig(
                    request.getOrganizationId(),
                    request.getRepositoryId(),
                    request.getBranch()
            );
            log.info("조직 레포지토리 설정: {}", configString);
        } else {
            // 개인 레포지토리
            configString = GitHubRepositoryConfigUtil.createPersonalRepoConfig(
                    request.getRepositoryId(),
                    request.getBranch()
            );
            log.info("개인 레포지토리 설정: {}", configString);
        }

        // 사용자 설정 업데이트
        user.setUploadRepository(configString);
        userRepository.save(user);

        log.info("기본 레포지토리 설정 완료 - 소유자: {}, 레포: {}, 브랜치: {}",
                owner, repoName, request.getBranch());

        // 응답 생성 (GitHub에서 조회한 실제 레포명 사용)
        return GitHubRepositorySettingDTO.RepositorySettingResponse.builder()
                .organizationId(request.getOrganizationId())
                .repositoryId(request.getRepositoryId())
                .repository(repoName)
                .branch(request.getBranch())
                .owner(owner)
                .isConfigured(true)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    /**
     * 기본 레포지토리 설정 조회
     */
    @Transactional(readOnly = true)
    public GitHubRepositorySettingDTO.RepositorySettingResponse getDefaultRepository(Long userId) {

        User user = entityValidator.getValidUserOrThrow(userId);

        if (user.getUploadRepository() == null || user.getUploadRepository().trim().isEmpty()) {
            // 설정되지 않은 경우
            return GitHubRepositorySettingDTO.RepositorySettingResponse.builder()
                    .isConfigured(false)
                    .build();
        }

        try {
            // 설정 파싱
            GitHubRepositoryConfigUtil.GitHubRepoConfig config =
                    GitHubRepositoryConfigUtil.parseConfig(user.getUploadRepository());

            // GitHub에서 실제 레포지토리 정보 조회
            gitHubApiUtils.validateToken(user);
            String token = gitHubApiUtils.decryptToken(user.getGithubToken());
            Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(config.getRepositoryId(), token);

            String repoName = repoInfo.get("name").toString();
            String owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();

            return GitHubRepositorySettingDTO.RepositorySettingResponse.builder()
                    .organizationId(config.isOrganization() ? config.getOrganizationId() : null)
                    .repositoryId(config.getRepositoryId())
                    .repository(repoName)
                    .branch(config.getBranch())
                    .owner(owner)
                    .isConfigured(true)
                    .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                    .build();

        } catch (GitHubException e) {
            throw e;
        } catch (Exception e) {
            log.warn("기본 레포지토리 설정 파싱 실패: {}", e.getMessage());
            return GitHubRepositorySettingDTO.RepositorySettingResponse.builder()
                    .isConfigured(false)
                    .build();
        }
    }
}

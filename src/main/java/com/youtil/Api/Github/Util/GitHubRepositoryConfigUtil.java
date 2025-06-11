package com.youtil.Api.Github.Util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub 레포지토리 설정 관련 유틸리티 클래스
 */
public class GitHubRepositoryConfigUtil {

    /**
     * 개인 레포지토리 설정 문자열 생성
     * 형식: repositoryId/branch
     */
    public static String createPersonalRepoConfig(Long repositoryId, String branch) {
        return repositoryId + "/" + branch;
    }

    /**
     * 조직 레포지토리 설정 문자열 생성
     * 형식: organizationId/repositoryId/branch
     */
    public static String createOrgRepoConfig(Long organizationId, Long repositoryId, String branch) {
        return organizationId + "/" + repositoryId + "/" + branch;
    }

    /**
     * 설정 문자열을 파싱하여 GitHubRepoConfig 객체로 변환
     */
    public static GitHubRepoConfig parseConfig(String uploadRepository) {
        if (uploadRepository == null || uploadRepository.trim().isEmpty()) {
            return null;
        }

        String[] parts = uploadRepository.split("/");

        if (parts.length == 2) {
            // 개인 레포지토리: repositoryId/branch
            return GitHubRepoConfig.builder()
                    .repositoryId(Long.parseLong(parts[0]))
                    .branch(parts[1])
                    .isOrganization(false)
                    .build();
        } else if (parts.length == 3) {
            // 조직 레포지토리: organizationId/repositoryId/branch
            return GitHubRepoConfig.builder()
                    .organizationId(Long.parseLong(parts[0]))
                    .repositoryId(Long.parseLong(parts[1]))
                    .branch(parts[2])
                    .isOrganization(true)
                    .build();
        }

        throw new IllegalArgumentException("잘못된 레포지토리 설정 형식: " + uploadRepository);
    }

    /**
     * GitHub 레포지토리 설정 정보를 담는 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubRepoConfig {
        private Long organizationId;
        private Long repositoryId;
        private String branch;
        private boolean isOrganization;
    }
}

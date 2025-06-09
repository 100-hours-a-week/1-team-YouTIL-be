package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.GitHubRepositorySettingDTO;
import com.youtil.Api.Github.Service.GitHubRepositorySettingService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

//@RestController
//@Tag(name = "github", description = "GitHub 설정 관련 API")
//@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubRepositorySettingController {

    private final GitHubRepositorySettingService gitHubRepositorySettingService;

    @Operation(
            summary = "기본 업로드 레포지토리 설정",
            description = "사용자의 기본 TIL 업로드 레포지토리를 설정합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "레포지토리 설정 성공",
                    content = @Content(schema = @Schema(implementation = GitHubRepositorySettingDTO.RepositorySettingResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "레포지토리를 찾을 수 없음"
            )
    })
    @PutMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<GitHubRepositorySettingDTO.RepositorySettingResponse>> setDefaultRepository(
            @RequestBody GitHubRepositorySettingDTO.SetRepositoryRequest request) {

        log.info("기본 레포지토리 설정 요청 - 조직 ID: {}, 레포지토리 ID: {}, 브랜치: {}",
                request.getOrganizationId(), request.getRepositoryId(), request.getBranch());

        try {
            // 요청 검증
            if (request.getRepositoryId() == null) {
                throw new IllegalArgumentException("레포지토리 ID는 필수입니다.");
            }

            if (request.getBranch() == null || request.getBranch().trim().isEmpty()) {
                throw new IllegalArgumentException("브랜치명은 필수입니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출
            GitHubRepositorySettingDTO.RepositorySettingResponse response =
                    gitHubRepositorySettingService.setDefaultRepository(request, userId);

            log.info("기본 레포지토리 설정 성공 - 소유자: {}, 레포: {}", response.getOwner(), response.getRepository());

            // 응답 생성
            ApiResponse<GitHubRepositorySettingDTO.RepositorySettingResponse> apiResponse = new ApiResponse<>(
                    "기본 업로드 레포지토리가 설정되었습니다.",
                    "GITHUB_REPO_SETTING_SUCCESS",
                    response);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("권한이 없습니다") || e.getMessage().contains("권한")) {
                log.warn("접근 권한 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            } else if (e.getMessage().contains("GitHub 토큰")) {
                log.warn("GitHub 토큰 문제: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
            } else {
                log.error("레포지토리 설정 오류: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "레포지토리 설정 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    @Operation(
            summary = "기본 레포지토리 설정 조회",
            description = "현재 설정된 기본 업로드 레포지토리 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "설정 조회 성공",
                    content = @Content(schema = @Schema(implementation = GitHubRepositorySettingDTO.RepositorySettingResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GitHubRepositorySettingDTO.RepositorySettingResponse>> getDefaultRepository() {

        log.info("기본 레포지토리 설정 조회 요청");

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출
            GitHubRepositorySettingDTO.RepositorySettingResponse response =
                    gitHubRepositorySettingService.getDefaultRepository(userId);

            if (response.getIsConfigured()) {
                log.info("기본 레포지토리 설정 조회 성공 - 레포: {}", response.getRepository());
                ApiResponse<GitHubRepositorySettingDTO.RepositorySettingResponse> apiResponse = new ApiResponse<>(
                        "기본 레포지토리 설정 조회 성공",
                        "GITHUB_REPO_SETTING_FETCHED",
                        response);
                return ResponseEntity.ok(apiResponse);
            } else {
                log.info("기본 레포지토리가 설정되지 않음");
                ApiResponse<GitHubRepositorySettingDTO.RepositorySettingResponse> apiResponse = new ApiResponse<>(
                        "기본 레포지토리가 설정되지 않았습니다.",
                        "GITHUB_REPO_NOT_CONFIGURED",
                        response);
                return ResponseEntity.ok(apiResponse);
            }

        } catch (Exception e) {
            log.error("기본 레포지토리 설정 조회 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "설정 조회 중 오류가 발생했습니다.");
        }
    }
}

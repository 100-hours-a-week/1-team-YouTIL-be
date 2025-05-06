package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCreateService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.TilMessageCode;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "tils", description = "TIL 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilCreateController {

    private final TilCreateService tilCreateService;
    private final GithubCommitDetailService githubCommitDetailService;
    private final TilAiService tilAiService;

    @Operation(
            summary = "TIL 생성",
            description = "커밋 정보에 기반한 AI 내용 생성 및 TIL 저장을 하나의 요청으로 처리합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "TIL 생성 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.CreateTilResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 유저가 존재하지 않습니다."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 입니다."
            )
    })
    @PostMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.CreateTilResponse>> createTil(
            @RequestBody TilRequestDTO.CreateWithAiRequest request) {

        log.info("TIL 생성 요청 - 레포지토리: {}, 제목: {}",
                request.getRepositoryId(), request.getTitle());

        try {
            // 요청 검증
            if (request.getRepositoryId() == null) {
                throw new IllegalArgumentException("레포지토리 ID가 필요합니다.");
            }
            if (request.getBranch() == null || request.getBranch().isEmpty()) {
                throw new IllegalArgumentException("브랜치명이 필요합니다.");
            }
            if (request.getCommits() == null || request.getCommits().isEmpty()) {
                throw new IllegalArgumentException("최소 하나 이상의 커밋 정보가 필요합니다.");
            }
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("TIL 제목이 필요합니다.");
            }
            if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException("TIL 카테고리가 필요합니다.");
            }
            if (request.getIsShared() == null) {
                throw new IllegalArgumentException("커뮤니티 업로드 여부가 필요합니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 1. GitHub 커밋 상세 정보 요청 객체 생성
            CommitDetailRequestDTO.CommitDetailRequest commitRequest = new CommitDetailRequestDTO.CommitDetailRequest();
            commitRequest.setRepositoryId(request.getRepositoryId());
            commitRequest.setOrganizationId(request.getOrganizationId());
            commitRequest.setBranch(request.getBranch());

            // CommitSummary를 CommitDetailRequestDTO.CommitSummary로 변환
            List<CommitDetailRequestDTO.CommitSummary> commitSummaries = request.getCommits().stream()
                    .map(commit -> CommitDetailRequestDTO.CommitSummary.builder()
                            .sha(commit.getSha())
                            .message(commit.getMessage())
                            .build())
                    .collect(Collectors.toList());

            commitRequest.setCommits(commitSummaries);

            // 2. GitHub에서 선택한 커밋의 상세 정보 조회
            CommitDetailResponseDTO.CommitDetailResponse commitDetail =
                    githubCommitDetailService.getCommitDetails(commitRequest, userId);

            // 조회된 파일 정보가 없는지 확인
            if (commitDetail.getFiles() == null || commitDetail.getFiles().isEmpty()) {
                throw new IllegalArgumentException("조회된 파일 정보가 없습니다.");
            }

            // 3. AI API로 TIL 내용 생성 요청 (branch 정보 추가 전달)
            TilAiResponseDTO aiResponse = tilAiService.generateTilContent(commitDetail, request.getRepositoryId(), request.getBranch());


            // 4. TIL 저장 요청 객체 생성
            TilRequestDTO.CreateAiTilRequest saveRequest = new TilRequestDTO.CreateAiTilRequest();
            // repo 필드 설정 - commitDetail.getRepo() 대신 request.getRepositoryId() 사용
            saveRequest.setRepo(String.valueOf(request.getRepositoryId()));
            saveRequest.setTitle(request.getTitle());
            saveRequest.setCategory(request.getCategory());
            saveRequest.setContent(aiResponse.getContent());
            saveRequest.setTags(aiResponse.getTags());
            saveRequest.setIsShared(request.getIsShared());

            // 5. TIL 저장
            TilResponseDTO.CreateTilResponse tilResponse = tilCreateService.createTilFromAi(saveRequest, userId);

            // 응답 생성 (TilMessageCode 사용)
            ApiResponse<TilResponseDTO.CreateTilResponse> response = new ApiResponse<>(
                    TilMessageCode.TIL_CREATED.getMessage(),
                    TilMessageCode.TIL_CREATED.getCode(),
                    tilResponse);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("TIL 생성 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TIL 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
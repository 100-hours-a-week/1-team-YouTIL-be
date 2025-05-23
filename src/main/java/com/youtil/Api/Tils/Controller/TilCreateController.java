package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCommendService;
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

    private final TilCommendService tilCommendService;
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
                throw new IllegalArgumentException(TilMessageCode.TIL_REPOSITORY_ID_REQUIRED.getMessage());
            }

            if (request.getBranch() == null || request.getBranch().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_BRANCH_REQUIRED.getMessage());
            }

            if (request.getCommits() == null || request.getCommits().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_COMMITS_REQUIRED.getMessage());
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_TITLE_REQUIRED.getMessage());
            }

            if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_CATEGORY_REQUIRED.getMessage());
            }

            if (request.getIsShared() == null) {
                throw new IllegalArgumentException(TilMessageCode.TIL_SHARED_STATUS_REQUIRED.getMessage());
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 1. GitHub 커밋 상세 정보 요청 객체 생성
            CommitDetailRequestDTO.CommitDetailRequest commitRequest = new CommitDetailRequestDTO.CommitDetailRequest();
            commitRequest.setRepositoryId(request.getRepositoryId());
            commitRequest.setOrganizationId(request.getOrganizationId());
            commitRequest.setBranch(request.getBranch());

            // CommitSummary를 CommitDetailRequestDTO.CommitSummary로 변환
            // GitHubDtoConverter 활용
            List<CommitDetailRequestDTO.CommitSummary> commitSummaries =
                    GitHubDtoConverter.toCommitDetailRequestSummaries(request.getCommits());
            commitRequest.setCommits(commitSummaries);

            // 2. GitHub에서 선택한 커밋의 상세 정보 조회
            CommitDetailResponseDTO.CommitDetailResponse commitDetail =
                    githubCommitDetailService.getCommitDetails(commitRequest, userId);

            // 조회된 파일 정보가 없는지 확인
            if (commitDetail.getFiles() == null || commitDetail.getFiles().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_FILES_NOT_FOUND.getMessage());
            }

            // 3. AI API로 TIL 내용 생성 요청 (title 정보 추가)
            TilAiResponseDTO aiResponse = tilAiService.generateTilContent(commitDetail, request.getRepositoryId(), request.getBranch(), request.getTitle());

            // 4. TIL 저장 요청 객체 생성 - TilDtoConverter 활용
            TilRequestDTO.CreateAiTilRequest saveRequest =
                    TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

            // 5. TIL 저장
            TilResponseDTO.CreateTilResponse tilResponse = tilCommendService.createTilFromAi(saveRequest, userId);

            // 응답 생성 (TilMessageCode 사용)
            ApiResponse<TilResponseDTO.CreateTilResponse> response = new ApiResponse<>(
                    TilMessageCode.TIL_CREATED.getMessage(),
                    TilMessageCode.TIL_CREATED.getCode(),
                    tilResponse);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResponseStatusException e) {
            // ResponseStatusException은 그대로 전파하여 적절한 HTTP 상태 코드 유지
            log.error("서비스 에러: {} - {}", e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("TIL 생성 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    TilMessageCode.TIL_CREATION_ERROR.getMessage() + ": " + e.getMessage());
        }
    }
}
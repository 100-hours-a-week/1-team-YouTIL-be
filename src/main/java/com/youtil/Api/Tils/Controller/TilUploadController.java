package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Tils.Dto.TilUploadRequestDTO;
import com.youtil.Api.Tils.Dto.TilUploadResponseDTO;
import com.youtil.Api.Tils.Service.TilUploadService;
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
import com.youtil.Common.DuplicatePrevention.PreventDuplicate;

@RestController
@Tag(name = "tils", description = "TIL 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilUploadController {

    private final TilUploadService tilUploadService;

    @Operation(
            summary = "TIL GitHub 업로드",
            description = "기본 설정된 레포지토리에 TIL을 마크다운 형태로 업로드합니다. 먼저 기본 레포지토리 설정이 필요합니다."
    )

    // @PreventDuplicate(action = "til_upload", dataFields = {"tilId"})
    @PostMapping(
            value = "/upload",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilUploadResponseDTO.UploadToGitHubResponse>> uploadTilToGitHub(
            @RequestBody TilUploadRequestDTO.UploadRequest request) {

        log.info("TIL GitHub 업로드 요청 - TIL ID: {}", request.getTilId());

        try {
            // 요청 검증
            if (request.getTilId() == null) {
                throw new IllegalArgumentException("TIL ID는 필수입니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출 (기본 설정 사용)
            TilUploadResponseDTO.UploadToGitHubResponse response =
                    tilUploadService.uploadTilToGitHub(request, userId);

            log.info("TIL GitHub 업로드 성공 - 파일 URL: {}", response.getFileUrl());

            // 응답 생성
            ApiResponse<TilUploadResponseDTO.UploadToGitHubResponse> apiResponse = new ApiResponse<>(
                    "TIL이 성공적으로 GitHub에 업로드되었습니다.",
                    "TIL_UPLOAD_SUCCESS",
                    response);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("기본 업로드 레포지토리가 설정되지 않았습니다")) {
                log.warn("기본 레포지토리 미설정: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "기본 업로드 레포지토리가 설정되지 않았습니다. 먼저 레포지토리를 설정해주세요.");
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("접근 권한이 없습니다") ||
                    e.getMessage().contains("권한이 없습니다")) {
                log.warn("접근 권한 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            } else if (e.getMessage().contains("GitHub 토큰")) {
                log.warn("GitHub 토큰 문제: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
            } else {
                log.error("TIL 업로드 오류: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "TIL 업로드 중 오류가 발생했습니다: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "서버 내부 오류가 발생했습니다.");
        }
    }
}

package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
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

import java.util.ArrayList;
import java.util.List;
import com.youtil.Common.DuplicatePrevention.PreventDuplicate;

@RestController
@Tag(name = "tils", description = "TIL 수정/삭제 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilUpdateDeleteController {

    private final TilCommendService tilCommendService;

    @Operation(
            summary = "TIL 수정",
            description = "현재 로그인한 사용자의 TIL을 수정합니다. 본인이 작성한 TIL만 수정할 수 있습니다."
    )

    @PreventDuplicate(action = "til_update", limitSeconds = 5, dataFields = {})
    @PutMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.TilDetailResponse>> updateTil(
            @RequestBody TilRequestDTO.UpdateTilRequest request) {

        log.info("TIL 수정 요청 - TIL ID: {}, 제목: {}", request.getTilId(), request.getTitle());

        try {
            // 요청 데이터 검증
            if (request.getTilId() == null) {
                throw new IllegalArgumentException("TIL ID는 필수입니다.");
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_TITLE_REQUIRED.getMessage());
            }

            // 제목 길이 검증 (DB 스키마에 따라 40자 제한)
            if (request.getTitle().length() > 40) {
                throw new IllegalArgumentException("TIL 제목은 40자를 초과할 수 없습니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 기존 TIL 정보를 먼저 조회해서 다른 필드들은 유지
            TilResponseDTO.TilDetailResponse existingTil = tilCommendService.getTilById(request.getTilId(), userId);

            // 새로운 UpdateTilRequest 생성 (제목만 변경, 나머지는 기존값 유지)
            TilRequestDTO.UpdateTilRequest fullUpdateRequest = new TilRequestDTO.UpdateTilRequest();
            fullUpdateRequest.setTilId(request.getTilId());
            fullUpdateRequest.setTitle(request.getTitle()); // 제목만 수정
            fullUpdateRequest.setContent(existingTil.getContent()); // 기존 내용 유지
            fullUpdateRequest.setCategory(existingTil.getCategory()); // 기존값 유지
            fullUpdateRequest.setTag(existingTil.getTag()); // 기존값 유지
            fullUpdateRequest.setIsDisplay(existingTil.getIsDisplay()); // 기존값 유지
            fullUpdateRequest.setCommitRepository(existingTil.getCommitRepository()); // 기존값 유지
            fullUpdateRequest.setIsUploaded(existingTil.getIsUploaded()); // 기존값 유지

            // TIL 수정
            TilResponseDTO.TilDetailResponse response = tilCommendService.updateTil(request.getTilId(), fullUpdateRequest, userId);

            // 응답 생성
            ApiResponse<TilResponseDTO.TilDetailResponse> apiResponse = new ApiResponse<>(
                    "TIL 제목이 성공적으로 수정되었습니다.",
                    "200",
                    response);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("TIL을 찾을 수 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        TilMessageCode.TIL_NOT_FOUND.getMessage());
            } else if (e.getMessage().contains("수정 권한이 없습니다")) {
                log.warn("TIL 수정 권한 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        TilMessageCode.TIL_EDIT_DENIED.getMessage());
            } else if (e.getMessage().contains("삭제된 TIL입니다")) {
                log.warn("삭제된 TIL: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.GONE,
                        TilMessageCode.TIL_ALREADY_DELETED.getMessage());
            } else {
                log.error("TIL 수정 중 오류: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        TilMessageCode.TIL_SERVER_ERROR.getMessage());
            }
        } catch (Exception e) {
            log.error("TIL 수정 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    TilMessageCode.TIL_SERVER_ERROR.getMessage() + ": " + e.getMessage());
        }
    }

    @Operation(
            summary = "TIL 삭제",
            description = "현재 로그인한 사용자의 TIL을 삭제합니다. 본인이 작성한 TIL만 삭제할 수 있습니다."
    )

    @PreventDuplicate(action = "til_delete", limitSeconds = 3, dataFields = {})
    @DeleteMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> deleteTils(
            @RequestBody TilRequestDTO.BatchDeleteTilRequest request) {

        log.info("TIL 삭제 요청 - TIL IDs: {}", request.getTilIds());

        try {
            // 요청 데이터 검증
            if (request.getTilIds() == null || request.getTilIds().isEmpty()) {
                throw new IllegalArgumentException("삭제할 TIL ID 목록은 필수입니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 1단계: 모든 TIL에 대해 권한 검증 먼저 수행 (실제 삭제 X)
            for (Long tilId : request.getTilIds()) {
                try {
                    // 권한 검증을 위해 TIL 조회만 수행
                    tilCommendService.getTilById(tilId, userId);
                } catch (RuntimeException e) {
                    // 권한이 없거나 찾을 수 없는 경우 즉시 전체 삭제 중단
                    if (e.getMessage().contains("삭제 권한이 없습니다") ||
                            e.getMessage().contains("권한이 없습니다") ||
                            e.getMessage().contains("수정 권한이 없습니다")) {
                        log.warn("TIL 삭제 권한 없음 - TIL ID: {}", tilId);
                        throw new RuntimeException("해당 TIL의 소유자가 아닙니다.");
                    } else if (e.getMessage().contains("찾을 수 없습니다")) {
                        log.warn("TIL을 찾을 수 없음 - TIL ID: {}", tilId);
                        throw new RuntimeException("삭제하려는 TIL을 찾을 수 없습니다.");
                    } else if (e.getMessage().contains("삭제된 TIL입니다")) {
                        log.warn("이미 삭제된 TIL - TIL ID: {}", tilId);
                        throw new RuntimeException("이미 삭제된 TIL이 포함되어 있습니다.");
                    } else {
                        throw e;
                    }
                }
            }

            // 2단계: 모든 권한 검증이 통과한 경우에만 실제 삭제 수행
            List<Long> deletedTilIds = new ArrayList<>();
            for (Long tilId : request.getTilIds()) {
                try {
                    tilCommendService.deleteTil(tilId, userId);
                    deletedTilIds.add(tilId);
                    log.info("TIL 삭제 성공 - TIL ID: {}", tilId);
                } catch (Exception e) {
                    // 이론적으로는 1단계에서 권한 검증을 했으므로 여기서 에러가 발생할 가능성은 낮음
                    log.error("TIL 삭제 중 예상치 못한 오류 - TIL ID: {}, 오류: {}", tilId, e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
            }

            // 성공 응답 생성
            ApiResponse<String> apiResponse = new ApiResponse<>(
                    "TIL 비활성화에 성공했습니다.",
                    "200",
                    null);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("TIL 삭제 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

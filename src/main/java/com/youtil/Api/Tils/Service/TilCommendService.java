package com.youtil.Api.Tils.Service;

import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilCommendService {

    private final TilRepository tilRepository;
    private final UserRepository userRepository;
    private final EntityValidator entityValidator;

    /**
     * AI가 생성한 TIL 저장
     */
    @Transactional
    public TilResponseDTO.CreateTilResponse createTilFromAi(
            TilRequestDTO.CreateAiTilRequest request, long userId) {
        // 사용자 조회
        User user = entityValidator.getValidUserOrThrow(userId);

        // 태그 처리 - TilDtoConverter 활용
        List<String> tags = TilDtoConverter.processTagList(request.getTags(),
                request.getCategory());

        // TIL 엔티티 생성 - TilDtoConverter 활용
        Til til = TilDtoConverter.createTilEntity(request, user, tags);

        // 저장
        Til savedTil = tilRepository.save(til);
        log.info(TilMessageCode.TIL_CREATED.getMessage() + " ID: {}", savedTil.getId());

        // 응답 DTO 생성
        return TilResponseDTO.CreateTilResponse.builder()
                .tilID(savedTil.getId())
                .build();
    }

    /**
     * 사용자의 TIL 목록 조회
     */
    @Transactional(readOnly = true)
    public TilResponseDTO.TilListResponse getUserTils(long userId, int page, int size) {
        // 사용자 존재 여부 확인
        entityValidator.getValidUserOrThrow(userId);

        // 페이징 처리된 TIL 목록 조회
        Pageable pageable = PageRequest.of(page, size);
        List<UserResponseDTO.TilListItem> tilItems = tilRepository.findUserTils(userId, pageable);

        // 응답 DTO 생성
        return TilResponseDTO.TilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * 특정 날짜의 사용자 TIL 목록 조회
     */
    @Transactional(readOnly = true)
    public TilResponseDTO.TilListResponse getUserTilsByDate(long userId, LocalDate date, int page,
            int size) {
        // 사용자 존재 여부 확인
        entityValidator.getValidUserOrThrow(userId);

        // 검색할 날짜 범위 설정 (해당 날짜의 00:00:00 ~ 23:59:59)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 페이징 처리된 특정 날짜의 TIL 목록 조회
        Pageable pageable = PageRequest.of(page, size);
        List<UserResponseDTO.TilListItem> tilItems = tilRepository.findUserTilsByDateRange(
                userId, startOfDay, endOfDay, pageable);

        // 응답 DTO 생성
        return TilResponseDTO.TilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * TIL 상세 조회
     */
    @Transactional
    public TilResponseDTO.TilDetailResponse getTilById(Long id, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(TilMessageCode.TIL_NOT_FOUND.getMessage()));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException(TilMessageCode.TIL_ALREADY_DELETED.getMessage());
        }

        // 비공개 TIL인 경우 본인 소유인지 확인
        if (!til.getIsDisplay() && !til.getUser().getId().equals(userId)) {
            throw new RuntimeException(TilMessageCode.TIL_ACCESS_DENIED.getMessage());
        }

        // 조회수 증가 (본인이 조회한 경우는 제외)
        if (!til.getUser().getId().equals(userId)) {
            til.setVisitedCount(til.getVisitedCount() + 1);
            tilRepository.save(til);
        }

        // DTO로 변환하여 반환 - TilDtoConverter 활용
        return TilDtoConverter.toTilDetailResponse(til);
    }

    /**
     * TIL 업데이트
     */
    @Transactional
    public TilResponseDTO.TilDetailResponse updateTil(Long id,
            TilRequestDTO.UpdateTilRequest request, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(TilMessageCode.TIL_NOT_FOUND.getMessage()));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException(TilMessageCode.TIL_ALREADY_DELETED.getMessage());
        }

        // 소유자 확인
        if (!til.getUser().getId().equals(userId)) {
            throw new RuntimeException(TilMessageCode.TIL_EDIT_DENIED.getMessage());
        }

        // 필드 업데이트
        til.setTitle(request.getTitle());
        til.setContent(request.getContent());
        til.setCategory(request.getCategory());
        til.setTag(request.getTag());
        til.setIsDisplay(request.getIsDisplay());
        til.setCommitRepository(request.getCommitRepository());
        til.setIsUploaded(request.getIsUploaded());

        // 저장
        Til updatedTil = tilRepository.save(til);

        // DTO로 변환하여 반환 - TilDtoConverter 활용
        return TilDtoConverter.toTilDetailResponse(updatedTil);
    }

    /**
     * TIL 삭제
     */
    @Transactional
    public void deleteTil(Long id, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(TilMessageCode.TIL_NOT_FOUND.getMessage()));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException(TilMessageCode.TIL_ALREADY_DELETED.getMessage());
        }

        // 소유자 확인
        if (!til.getUser().getId().equals(userId)) {
            throw new RuntimeException(TilMessageCode.TIL_DELETE_DENIED.getMessage());
        }

        // 논리적 삭제 처리
        til.setStatus(Status.deactive);
        til.setDeletedAt(LocalDateTime.now());
        tilRepository.save(til);
    }


}
package com.youtil.Api.Tils.Service;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilCreateService {

    private final TilRepository tilRepository;
    private final UserRepository userRepository;
    private final EntityValidator entityValidator;

    /**
     * TIL 생성
     */
    @Transactional
    public TilResponseDTO.CreateTilResponse createTil(TilRequestDTO.CreateTilRequest request, long userId) {
        // 사용자 조회
        User user = entityValidator.getValidUserOrThrow(userId);

        // 태그 생성 (카테고리를 기본 태그로 추가)
        List<String> tags = new ArrayList<>();
        tags.add(request.getCategory());

        // 커밋 정보에서 내용 추출
        StringBuilder contentBuilder = new StringBuilder();
        if (request.getCommits() != null) {
            for (TilRequestDTO.CommitItem commit : request.getCommits()) {
                // 커밋 메시지 추가
                contentBuilder.append("## Commit: ").append(commit.getMessage()).append("\n\n");

                // 변경사항 추가
                if (commit.getChanges() != null) {
                    for (TilRequestDTO.ChangeItem change : commit.getChanges()) {
                        contentBuilder.append("### File: ").append(change.getFilename()).append("\n\n");
                        contentBuilder.append("```\n");
                        contentBuilder.append(change.getPatch()).append("\n");
                        contentBuilder.append("```\n\n");
                    }
                }

                contentBuilder.append("---\n\n");
            }
        }

        String content = contentBuilder.toString();

        // TIL 엔티티 생성
        Til til = Til.builder()
                .user(user)
                .title(request.getTitle())
                .content(content)
                .category(request.getCategory())
                .tag(tags)
                .isDisplay(true) // 항상 표시
                .commitRepository(request.getRepo())
                .isUploaded(request.getIsShared())
                .recommendCount(0)
                .visitedCount(0)
                .commentsCount(0)
                .status(Status.active)
                .build();

        // 저장
        Til savedTil = tilRepository.save(til);
        log.info("TIL이 성공적으로 생성되었습니다. ID: {}", savedTil.getId());

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
     * TIL 상세 조회
     */
    @Transactional
    public TilResponseDTO.TilDetailResponse getTilById(Long id, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException("삭제된 TIL입니다.");
        }

        // 비공개 TIL인 경우 본인 소유인지 확인
        if (!til.getIsDisplay() && !til.getUser().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // 조회수 증가 (본인이 조회한 경우는 제외)
        if (!til.getUser().getId().equals(userId)) {
            til.setVisitedCount(til.getVisitedCount() + 1);
            tilRepository.save(til);
        }

        // DTO로 변환하여 반환
        return mapToDetailResponse(til);
    }

    /**
     * TIL 업데이트
     */
    @Transactional
    public TilResponseDTO.TilDetailResponse updateTil(Long id, TilRequestDTO.UpdateTilRequest request, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException("삭제된 TIL입니다.");
        }

        // 소유자 확인
        if (!til.getUser().getId().equals(userId)) {
            throw new RuntimeException("TIL 수정 권한이 없습니다.");
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

        // DTO로 변환하여 반환
        return mapToDetailResponse(updatedTil);
    }

    /**
     * TIL 삭제
     */
    @Transactional
    public void deleteTil(Long id, long userId) {
        // TIL 조회
        Til til = tilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TIL을 찾을 수 없습니다."));

        // 삭제된 TIL인지 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException("이미 삭제된 TIL입니다.");
        }

        // 소유자 확인
        if (!til.getUser().getId().equals(userId)) {
            throw new RuntimeException("TIL 삭제 권한이 없습니다.");
        }

        // 논리적 삭제 처리
        til.setStatus(Status.deactive);
        til.setDeletedAt(LocalDateTime.now());
        tilRepository.save(til);
    }

    /**
     * 연도별 TIL 목록 조회
     */
    @Transactional(readOnly = true)
    public TilResponseDTO.TilYearListResponse getTilsByYear(long userId, int year) {
        // 사용자 존재 여부 확인
        entityValidator.getValidUserOrThrow(userId);

        // 연도별 TIL 목록 조회
        List<Til> tils = tilRepository.findAllByUserIdAndYear(userId, year);

        // DTO 리스트로 변환
        List<TilResponseDTO.TilYearItem> tilItems = tils.stream()
                .filter(til -> til.getStatus() != Status.deactive) // 삭제된 TIL 제외
                .map(this::mapToYearItem)
                .collect(Collectors.toList());

        // 응답 DTO 생성
        return TilResponseDTO.TilYearListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * Entity를 상세 응답 DTO로 변환
     */
    private TilResponseDTO.TilDetailResponse mapToDetailResponse(Til til) {
        return TilResponseDTO.TilDetailResponse.builder()
                .id(til.getId())
                .userId(til.getUser().getId())
                .nickname(til.getUser().getNickname())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .title(til.getTitle())
                .content(til.getContent())
                .category(til.getCategory())
                .tag(til.getTag())
                .isDisplay(til.getIsDisplay())
                .commitRepository(til.getCommitRepository())
                .isUploaded(til.getIsUploaded())
                .recommendCount(til.getRecommendCount())
                .visitedCount(til.getVisitedCount())
                .commentsCount(til.getCommentsCount())
                .createdAt(til.getCreatedAt())
                .updatedAt(til.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 연도별 목록 아이템 DTO로 변환
     */
    private TilResponseDTO.TilYearItem mapToYearItem(Til til) {
        return TilResponseDTO.TilYearItem.builder()
                .id(til.getId())
                .title(til.getTitle())
                .category(til.getCategory())
                .tag(til.getTag())
                .createdAt(til.getCreatedAt())
                .build();
    }
}
package com.youtil.Api.Guestbook.Service;

import com.youtil.Api.Guestbook.Converter.GuestbookConverter;
import com.youtil.Api.Guestbook.dto.GuestbookRequestDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO.GuestbookItem;
import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Exception.GuestbookException.GuestbookException;
import com.youtil.Model.Guestbook;
import com.youtil.Repository.GuestbookRepository;
import com.youtil.Util.EntityValidator;
import com.youtil.Util.GuestbookValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final EntityValidator entityValidator;

    @Transactional
    public GuestbookResponseDTO.CreateGuestbookResponseDTO createGuestbook(Long ownerId, Long guestId,
                                                                           GuestbookRequestDTO.CreateGuestbookRequestDTO request) {
        // 유효성 검증들을 통합된 유틸리티로 처리
        validateUsersExist(ownerId, guestId);

        // 답글인 경우 상위 방명록 유효성 검증
        if (request.getTopGuestbookId() != null) {
            validateParentGuestbook(request.getTopGuestbookId(), ownerId);
        }

        Guestbook guestbook = GuestbookConverter.toGuestbook(ownerId, guestId, request);
        Guestbook savedGuestbook = guestbookRepository.save(guestbook);

        log.info("방명록 생성 완료 - ID: {}, 작성자: {}, 대상: {}",
                savedGuestbook.getId(), guestId, ownerId);

        return GuestbookConverter.toCreateGuestbookResponseDTO(savedGuestbook.getId());
    }

    @Transactional(readOnly = true)
    public GuestbookResponseDTO.GetGuestbookListResponseDTO getGuestbookList(
            Long ownerId, Pageable pageable) {
        // 방명록 주인 유효성 검증
        entityValidator.getValidUserOrThrow(ownerId);

        // 최상위 방명록들 조회 (페이징)
        Page<Guestbook> topLevelGuestbooks = guestbookRepository
                .findTopLevelGuestbooksByOwnerId(ownerId, GuestbookStatus.ACTIVE, pageable);

        if (topLevelGuestbooks.isEmpty()) {
            log.debug("사용자 {}의 방명록이 없습니다.", ownerId);
        }

        // 답글까지 포함된 방명록 리스트 구성
        List<GuestbookItem> guestbooksWithReplies = buildGuestbookListWithReplies(topLevelGuestbooks);

        // Page<GuestbookItem>을 생성하기 위해 topLevelGuestbooks를 변환
        Page<GuestbookItem> guestbookItemPage = topLevelGuestbooks.map(GuestbookConverter::toGuestbookItem);

        return GuestbookConverter.toGuestbookListResponseDTO(
                guestbookItemPage, guestbooksWithReplies);
    }

    @Transactional
    public void updateGuestbook(Long ownerId, Long guestbookId, Long guestId,
                                GuestbookRequestDTO.UpdateGuestbookRequestDTO request) {
        // 방명록 주인이 실제 존재하는 사용자인지 검증
        entityValidator.getValidUserOrThrow(ownerId);

        // 방명록 작성자가 실제 존재하는 사용자인지 검증
        entityValidator.getValidUserOrThrow(guestId);

        // 방명록 조회 및 권한 확인
        Guestbook guestbook = getGuestbookWithPermissionCheck(guestbookId, guestId);

        // 방명록이 해당 주인의 방명록인지 확인
        if (!guestbook.getOwnerId().equals(ownerId)) {
            throw new GuestbookException.InvalidGuestbookAccessException();
        }

        // 삭제된 댓글은 수정할 수 없음
        if (guestbook.isDeleted()) {
            throw new GuestbookException("삭제된 댓글은 수정할 수 없습니다.");
        }

        // 내용 업데이트
        guestbook.setContent(request.getContent());

        log.info("방명록 수정 완료 - ID: {}, 수정자: {}, 주인: {}", guestbookId, guestId, ownerId);
    }

    @Transactional
    public void deleteGuestbook(Long ownerId, Long guestbookId, Long guestId) {
        // 방명록 주인이 실제 존재하는 사용자인지 검증
        entityValidator.getValidUserOrThrow(ownerId);

        // 방명록 작성자가 실제 존재하는 사용자인지 검증
        entityValidator.getValidUserOrThrow(guestId);

        // 방명록 조회 및 권한 확인
        Guestbook guestbook = getGuestbookWithPermissionCheck(guestbookId, guestId);

        // 방명록이 해당 주인의 방명록인지 확인
        if (!guestbook.getOwnerId().equals(ownerId)) {
            throw new GuestbookException.InvalidGuestbookAccessException();
        }

        // 스마트 삭제 실행
        performSmartDelete(guestbook);

        log.info("방명록 삭제 완료 - ID: {}, 삭제자: {}, 주인: {}, 삭제 방식: {}",
                guestbookId, guestId, ownerId, guestbook.isActive() ? "내용만 삭제" : "완전 삭제");
    }

    // =========================== Private Helper Methods ===========================

    /**
     * 사용자들 존재 여부 검증
     */
    private void validateUsersExist(Long ownerId, Long guestId) {
        entityValidator.getValidUserOrThrow(ownerId);
        entityValidator.getValidUserOrThrow(guestId);
    }

    /**
     * 상위 방명록 유효성 검증
     */
    private void validateParentGuestbook(Long parentGuestbookId, Long ownerId) {
        Guestbook parentGuestbook = guestbookRepository
                .findByIdAndStatus(parentGuestbookId, GuestbookStatus.ACTIVE)
                .orElseThrow(GuestbookException.InvalidParentGuestbookException::new);

        // 상위 방명록의 주인이 현재 요청한 주인과 같은지 확인
        if (!parentGuestbook.getOwnerId().equals(ownerId)) {
            throw new GuestbookException.InvalidGuestbookAccessException();
        }

        // 2단계 이상 답글 방지 (답글의 답글 금지)
        if (parentGuestbook.isReply()) {
            throw new GuestbookException.GuestbookReplyDepthExceededException();
        }

        // 삭제된 댓글에는 답글을 달 수 없음
        if (parentGuestbook.isDeleted()) {
            throw new GuestbookException.CannotReplyToDeletedGuestbookException();
        }
    }

    /**
     * 권한 확인과 함께 방명록 조회
     */
    private Guestbook getGuestbookWithPermissionCheck(Long guestbookId, Long guestId) {
        return guestbookRepository
                .findByIdAndGuestIdAndStatus(guestbookId, guestId, GuestbookStatus.ACTIVE)
                .orElseThrow(GuestbookException.GuestbookNotFoundException::new);
    }

    /**
     * 스마트 삭제 수행
     * - 대댓글이 있는 원댓글: 내용만 "삭제된 댓글입니다"로 변경
     * - 대댓글이 없는 원댓글 또는 대댓글: 완전 삭제 (소프트 삭제)
     */
    private void performSmartDelete(Guestbook guestbook) {
        if (guestbook.isTopLevel()) {
            // 최상위 댓글인 경우
            long activeRepliesCount = guestbookRepository
                    .countActiveRepliesByTopGuestbookId(guestbook.getId(), GuestbookStatus.ACTIVE);

            if (activeRepliesCount > 0) {
                // 대댓글이 있으면 내용만 삭제
                guestbook.markAsDeleted();
                log.debug("원댓글 내용만 삭제 - ID: {}, 대댓글 수: {}", guestbook.getId(), activeRepliesCount);
            } else {
                // 대댓글이 없으면 완전 삭제
                guestbook.softDelete();
                log.debug("원댓글 완전 삭제 - ID: {}", guestbook.getId());
            }
        } else {
            // 대댓글인 경우 항상 완전 삭제
            guestbook.softDelete();
            log.debug("대댓글 완전 삭제 - ID: {}", guestbook.getId());
        }
    }

    /**
     * 답글까지 포함된 방명록 리스트 구성
     */
    private List<GuestbookItem> buildGuestbookListWithReplies(Page<Guestbook> topLevelGuestbooks) {
        // 각 최상위 방명록의 답글들 조회
        List<Long> topLevelIds = topLevelGuestbooks.getContent().stream()
                .map(Guestbook::getId)
                .collect(Collectors.toList());

        if (topLevelIds.isEmpty()) {
            return topLevelGuestbooks.getContent().stream()
                    .map(GuestbookConverter::toGuestbookItem)
                    .collect(Collectors.toList());
        }

        Map<Long, List<GuestbookItem>> repliesMap = topLevelIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getRepliesForGuestbook
                ));

        // 답글을 각 최상위 방명록에 매핑 (원본 Entity 사용)
        return topLevelGuestbooks.getContent().stream()
                .map(guestbook -> GuestbookItem.builder()
                        .id(guestbook.getId())
                        .guestId(guestbook.getGuestId())
                        .guestNickname(guestbook.getGuest() != null ? guestbook.getGuest().getNickname() : "알 수 없는 사용자")
                        .guestProfileImageUrl(guestbook.getGuest() != null ? guestbook.getGuest().getProfileImageUrl() : null)
                        .content(guestbook.getContent())
                        .topGuestbookId(guestbook.getTopGuestbookId())
                        .createdAt(guestbook.getCreatedAt())
                        .updatedAt(guestbook.getUpdatedAt())
                        .deleted(guestbook.isDeleted())  // 원본 Guestbook 엔티티에서 삭제 상태 확인
                        .replies(repliesMap.get(guestbook.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 방명록의 답글들 조회
     */
    private List<GuestbookItem> getRepliesForGuestbook(Long guestbookId) {
        List<Guestbook> replies = guestbookRepository
                .findRepliesByTopGuestbookId(guestbookId, GuestbookStatus.ACTIVE);
        return replies.stream()
                .map(GuestbookConverter::toGuestbookItem)
                .collect(Collectors.toList());
    }
}

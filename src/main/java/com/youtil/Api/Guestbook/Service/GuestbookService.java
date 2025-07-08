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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final EntityValidator entityValidator;

    @Transactional
    public GuestbookResponseDTO.CreateGuestbookResponseDTO createGuestbook(Long ownerId,
                                                                           Long guestId,
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

        // 최상위 방명록들 조회 (기존 메서드 사용)
        Page<Guestbook> topLevelGuestbooks = guestbookRepository
                .findTopLevelGuestbooksByOwnerId(ownerId, GuestbookStatus.ACTIVE, pageable);

        if (topLevelGuestbooks.isEmpty()) {
            log.debug("사용자 {}의 방명록이 없습니다.", ownerId);
        }

        // 답글까지 포함된 방명록 리스트 구성
        List<GuestbookItem> guestbooksWithReplies = buildGuestbookListWithReplies(
                topLevelGuestbooks);

        // Page<GuestbookItem>을 생성하기 위해 topLevelGuestbooks를 변환
        Page<GuestbookItem> guestbookItemPage = topLevelGuestbooks.map(
                this::convertToGuestbookItemForPaging);

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
    public void deleteGuestbook(Long ownerId, Long guestbookId, Long userId) {
        entityValidator.getValidUserOrThrow(ownerId);
        entityValidator.getValidUserOrThrow(userId);

        Guestbook guestbook = guestbookRepository.findById(guestbookId)
                .orElseThrow(GuestbookException.GuestbookNotFoundException::new);

        boolean isGuest = guestbook.getGuestId().equals(userId);
        boolean isOwner = guestbook.getOwnerId().equals(userId);

        if (!isGuest && !isOwner) {
            throw new GuestbookException.InvalidGuestbookAccessException();
        }

        //작성자가 우선이므로, 작성자인 경우 deletedByOwner = false 고정
        boolean deletedByOwner = !isGuest && isOwner;

        performSmartDelete(guestbook, deletedByOwner);

        String deleteType = guestbook.isDeleted() ? "내용만 삭제" : "완전 삭제";
        log.info("방명록 삭제 완료 - ID: {}, 삭제자: {}, 주인: {}, 삭제 방식: {}, 삭제 주체: {}",
                guestbookId, userId, ownerId, deleteType, deletedByOwner ? "프로필 주인" : "작성자");
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
     * 상위 방명록 유효성 검증 (삭제된 방명록에도 대댓글 추가 가능하도록 수정)
     */
    private void validateParentGuestbook(Long parentGuestbookId, Long ownerId) {
        // 1. 상위 방명록 존재 여부 확인 (삭제 상태 무관하게 조회)
        Guestbook parentGuestbook = guestbookRepository
                .findByIdIgnoreStatus(parentGuestbookId)
                .orElseThrow(GuestbookException.InvalidParentGuestbookException::new);

        // 2. 상위 방명록의 주인이 현재 요청한 주인과 같은지 확인
        if (!parentGuestbook.getOwnerId().equals(ownerId)) {
            throw new GuestbookException.InvalidGuestbookAccessException();
        }

        // 3. 2단계 이상 답글 방지 (답글의 답글 금지)
        if (parentGuestbook.isReply()) {
            throw new GuestbookException.GuestbookReplyDepthExceededException();
        }

        // 4. 완전 삭제된 방명록에만 대댓글 추가 불가 (DEACTIVE 상태만)
        if (parentGuestbook.getStatus() == GuestbookStatus.DEACTIVE) {
            throw new GuestbookException.CannotReplyToDeletedGuestbookException();
        }

        // 5. 내용만 삭제된 방명록(ACTIVE + "삭제된 댓글입니다")에는 대댓글 추가 가능!
        if (parentGuestbook.getStatus() == GuestbookStatus.ACTIVE) {
            if (GuestbookStatus.DELETED_COMMENT_MESSAGE.equals(parentGuestbook.getContent())) {
                log.debug("내용만 삭제된 방명록에 대댓글 추가 허용 - 상위 방명록 ID: {}", parentGuestbookId);
            } else {
                log.debug("일반 활성 방명록에 대댓글 추가 - 상위 방명록 ID: {}", parentGuestbookId);
            }
        }

        log.debug("상위 방명록 검증 완료 - ID: {}, 상태: {}, 대댓글 추가 가능",
                parentGuestbookId, parentGuestbook.getStatus());
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
     * - 대댓글인 경우: 완전 삭제 후 원댓글 자동 삭제 검사
     * - 대댓글이 있는 원댓글: 내용만 "삭제된 댓글입니다"로 변경 (상태는 ACTIVE 유지)
     * - 대댓글이 없는 원댓글: 완전 삭제 (소프트 삭제)
     */
    private void performSmartDelete(Guestbook guestbook, Boolean deletedByOwner) {
        if (guestbook.isTopLevel()) {
            // 최상위 댓글인 경우
            long activeRepliesCount = guestbookRepository
                    .countActiveRepliesByTopGuestbookId(guestbook.getId(), GuestbookStatus.ACTIVE);

            if (activeRepliesCount > 0) {
                // 대댓글이 있으면 내용만 삭제 (상태는 ACTIVE로 유지하여 대댓글 추가 가능)
                guestbook.markAsDeleted(
                        deletedByOwner ? GuestbookStatus.DELETED_BY_OWNER_MESSAGE
                                : GuestbookStatus.DELETED_COMMENT_MESSAGE
                );
                log.debug("원댓글 내용만 삭제 - ID: {}, 대댓글 수: {}, 대댓글 추가 여전히 가능",
                        guestbook.getId(), activeRepliesCount);
            } else {
                // 대댓글이 없으면 완전 삭제
                guestbook.softDelete();
                log.debug("원댓글 완전 삭제 - ID: {}", guestbook.getId());
            }
        } else {
            // 대댓글인 경우 항상 완전 삭제
            Long parentGuestbookId = guestbook.getTopGuestbookId();
            guestbook.softDelete();
            log.debug("대댓글 완전 삭제 - ID: {}", guestbook.getId());

            // 대댓글 삭제 후 원댓글 자동 삭제 검사 수행
            checkAndDeleteParentIfNoReplies(parentGuestbookId);
        }
    }

    /**
     * 대댓글이 모두 삭제되었을 때 원댓글도 자동 삭제하는 메서드
     * - 원댓글이 내용 삭제 상태("삭제된 댓글입니다")이고
     * - 활성 대댓글이 없으면 원댓글도 완전 삭제
     */
    private void checkAndDeleteParentIfNoReplies(Long parentGuestbookId) {
        if (parentGuestbookId == null) {
            return; // 최상위 댓글이므로 체크할 필요 없음
        }

        try {
            // 원댓글 조회 (삭제 상태 무관)
            Guestbook parentGuestbook = guestbookRepository
                    .findByIdIgnoreStatus(parentGuestbookId)
                    .orElse(null);

            if (parentGuestbook == null) {
                log.debug("원댓글을 찾을 수 없음 - ID: {}", parentGuestbookId);
                return;
            }

            // 이미 완전 삭제된 원댓글이면 처리하지 않음
            if (parentGuestbook.getStatus() == GuestbookStatus.DEACTIVE) {
                log.debug("이미 완전 삭제된 원댓글 - ID: {}", parentGuestbookId);
                return;
            }

            // 내용만 삭제된 상태가 아니면 처리하지 않음
            if (!parentGuestbook.isDeleted()) {
                log.debug("내용 삭제 상태가 아닌 원댓글 - ID: {}, 내용: {}",
                        parentGuestbookId, parentGuestbook.getContent());
                return;
            }

            // 활성 대댓글 개수 확인
            long activeRepliesCount = guestbookRepository
                    .countActiveRepliesByTopGuestbookId(parentGuestbookId, GuestbookStatus.ACTIVE);

            if (activeRepliesCount == 0) {
                // 활성 대댓글이 없으면 원댓글도 완전 삭제
                parentGuestbook.softDelete();
                log.info("대댓글이 모두 삭제되어 원댓글도 자동 삭제됨 - 원댓글 ID: {}", parentGuestbookId);
            } else {
                log.debug("아직 활성 대댓글이 존재함 - 원댓글 ID: {}, 대댓글 수: {}",
                        parentGuestbookId, activeRepliesCount);
            }

        } catch (Exception e) {
            log.error("원댓글 자동 삭제 검사 중 오류 발생 - 원댓글 ID: {}, 오류: {}",
                    parentGuestbookId, e.getMessage(), e);
            // 원댓글 자동 삭제 실패해도 대댓글 삭제는 이미 완료된 상태이므로 예외를 던지지 않음
        }
    }

    /**
     * 페이징용 Guestbook을 GuestbookItem으로 변환 (간단한 변환)
     */
    private GuestbookItem convertToGuestbookItemForPaging(Guestbook guestbook) {
        return GuestbookConverter.toGuestbookItem(guestbook);
    }

    /**
     * 답글까지 포함된 방명록 리스트 구성 (삭제된 방명록 처리 포함)
     */
    private List<GuestbookItem> buildGuestbookListWithReplies(Page<Guestbook> topLevelGuestbooks) {
        // 각 최상위 방명록의 답글들 조회
        List<Long> topLevelIds = topLevelGuestbooks.getContent().stream()
                .map(Guestbook::getId)
                .collect(Collectors.toList());

        if (topLevelIds.isEmpty()) {
            return topLevelGuestbooks.getContent().stream()
                    .map(this::convertToDetailedGuestbookItem)
                    .collect(Collectors.toList());
        }

        Map<Long, List<GuestbookItem>> repliesMap = topLevelIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getRepliesForGuestbook
                ));

        // 답글을 각 최상위 방명록에 매핑
        return topLevelGuestbooks.getContent().stream()
                .map(guestbook -> {
                    GuestbookItem item = convertToDetailedGuestbookItem(guestbook);
                    // replies 설정하여 새로운 객체 생성
                    return GuestbookItem.builder()
                            .id(item.getId())
                            .guestId(item.getGuestId())
                            .guestNickname(item.getGuestNickname())
                            .guestProfileImageUrl(item.getGuestProfileImageUrl())
                            .content(item.getContent())
                            .topGuestbookId(item.getTopGuestbookId())
                            .createdAt(item.getCreatedAt())
                            .updatedAt(item.getUpdatedAt())
                            .deleted(item.isDeleted())
                            .replies(repliesMap.get(guestbook.getId()))  // 대댓글 설정
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 상세한 Guestbook을 GuestbookItem으로 변환 (삭제된 방명록 처리 포함)
     */
    private GuestbookItem convertToDetailedGuestbookItem(Guestbook guestbook) {
        return GuestbookItem.builder()
                .id(guestbook.getId())
                .guestId(guestbook.getGuestId())
                .guestNickname(guestbook.getGuest() != null
                        ? guestbook.getGuest().getNickname()
                        : "알 수 없는 사용자")
                .guestProfileImageUrl(guestbook.getGuest() != null
                        ? guestbook.getGuest().getProfileImageUrl()
                        : null)
                .content(guestbook.getContent())  // 삭제 메시지가 이미 DB에 저장된 상태로 전달됨
                .topGuestbookId(guestbook.getTopGuestbookId())
                .createdAt(guestbook.getCreatedAt())
                .updatedAt(guestbook.getUpdatedAt())
                .deleted(guestbook.isDeleted())
                .replies(null)
                .build();
    }

    /**
     * 특정 방명록의 답글들 조회
     */
    private List<GuestbookItem> getRepliesForGuestbook(Long guestbookId) {
        List<Guestbook> replies = guestbookRepository
                .findRepliesByTopGuestbookId(guestbookId, GuestbookStatus.ACTIVE);
        return replies.stream()
                .map(this::convertToDetailedGuestbookItem)
                .collect(Collectors.toList());
    }
}

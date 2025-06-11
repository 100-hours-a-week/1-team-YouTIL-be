package com.youtil.Repository;

import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Model.Guestbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestbookRepository extends JpaRepository<Guestbook, Long> {

    // 특정 유저의 방명록 조회 (페이징, 최상위 댓글만) - Entity로 반환
    @Query("SELECT g FROM Guestbook g " +
            "LEFT JOIN FETCH g.guest " +
            "WHERE g.ownerId = :ownerId AND g.status = :status AND g.topGuestbookId IS NULL " +
            "ORDER BY g.createdAt DESC")
    Page<Guestbook> findTopLevelGuestbooksByOwnerId(@Param("ownerId") Long ownerId,
                                                    @Param("status") GuestbookStatus status,
                                                    Pageable pageable);

    // 특정 최상위 방명록의 답글들 조회 - Entity로 반환
    @Query("SELECT g FROM Guestbook g " +
            "LEFT JOIN FETCH g.guest " +
            "WHERE g.topGuestbookId = :topGuestbookId AND g.status = :status " +
            "ORDER BY g.createdAt ASC")
    List<Guestbook> findRepliesByTopGuestbookId(@Param("topGuestbookId") Long topGuestbookId,
                                                @Param("status") GuestbookStatus status);

    // 특정 유저의 활성화된 방명록 총 개수 조회
    long countByOwnerIdAndStatus(Long ownerId, GuestbookStatus status);

    // ID와 게스트 ID로 방명록 조회 (수정/삭제 권한 확인용)
    Optional<Guestbook> findByIdAndGuestIdAndStatus(Long id, Long guestId, GuestbookStatus status);

    // ID와 상태로 방명록 조회
    Optional<Guestbook> findByIdAndStatus(Long id, GuestbookStatus status);

    // 특정 유저가 작성한 방명록들 조회
    List<Guestbook> findByGuestIdAndStatus(Long guestId, GuestbookStatus status);

    // 특정 방명록의 활성 상태 대댓글 개수 조회
    @Query("SELECT COUNT(g) FROM Guestbook g " +
            "WHERE g.topGuestbookId = :topGuestbookId AND g.status = :status")
    long countActiveRepliesByTopGuestbookId(@Param("topGuestbookId") Long topGuestbookId,
                                            @Param("status") GuestbookStatus status);

    // 특정 방명록의 모든 대댓글 조회 (삭제된 것도 포함)
    @Query("SELECT g FROM Guestbook g " +
            "WHERE g.topGuestbookId = :topGuestbookId " +
            "ORDER BY g.createdAt ASC")
    List<Guestbook> findAllRepliesByTopGuestbookId(@Param("topGuestbookId") Long topGuestbookId);

    /**
     * ID로 방명록 조회 (삭제 상태 무관)
     * - 삭제된 방명록에 대댓글 추가할 때 상위 방명록 존재 여부 확인용
     * - validateParentGuestbook에서 사용
     */
    @Query("SELECT g FROM Guestbook g " +
            "LEFT JOIN FETCH g.guest " +
            "WHERE g.id = :id")
    Optional<Guestbook> findByIdIgnoreStatus(@Param("id") Long id);

    /**
     * 특정 유저의 최상위 방명록들 조회 (내용만 삭제된 것도 포함)
     * - 완전 삭제(DEACTIVE)된 것은 제외
     * - 내용만 삭제된 것(ACTIVE + content="삭제된 댓글입니다")은 포함
     * - 삭제된 방명록도 목록에 표시하되 대댓글 추가는 가능하도록
     */
    @Query("SELECT g FROM Guestbook g " +
            "LEFT JOIN FETCH g.guest " +
            "WHERE g.ownerId = :ownerId " +
            "AND g.status = :status " +  // ACTIVE 상태만
            "AND g.topGuestbookId IS NULL " +
            "ORDER BY g.createdAt DESC")
    Page<Guestbook> findTopLevelGuestbooksIncludingContentDeleted(@Param("ownerId") Long ownerId,
                                                                  @Param("status") GuestbookStatus status,
                                                                  Pageable pageable);

    /**
     * 특정 방명록이 내용만 삭제된 상태인지 확인
     * - content가 "삭제된 댓글입니다"인 ACTIVE 상태 방명록
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END " +
            "FROM Guestbook g " +
            "WHERE g.id = :id " +
            "AND g.status = :activeStatus " +
            "AND g.content = :deletedMessage")
    boolean isContentDeletedGuestbook(@Param("id") Long id,
                                      @Param("activeStatus") GuestbookStatus activeStatus,
                                      @Param("deletedMessage") String deletedMessage);

    /**
     * 특정 방명록이 완전 삭제된 상태인지 확인
     * - status가 DEACTIVE인 방명록
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END " +
            "FROM Guestbook g " +
            "WHERE g.id = :id " +
            "AND g.status = :deactiveStatus")
    boolean isFullyDeletedGuestbook(@Param("id") Long id,
                                    @Param("deactiveStatus") GuestbookStatus deactiveStatus);
}

package com.youtil.Repository;

import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Model.Til;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TilRepositoryCustom {

    /**
     * 특정 사용자의 특정 연도 TIL 목록 조회
     */
    List<Til> findAllByUserIdAndYear(long userId, int year);

    /**
     * 특정 사용자의 TIL 목록 조회 (페이징)
     */
    List<TilListItem> findUserTils(Long userId, Pageable pageable);

    /**
     * 특정 날짜 범위 내의 사용자 TIL 목록 조회 (페이징)
     */
    List<TilListItem> findUserTilsByDateRange(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime, Pageable pageable);

    /**
     * 최신 공개 TIL 목록 조회 (페이징)
     * 활성화 상태(active)이고, 공개 설정(isDisplay=true)된 TIL만 조회
     */
    List<Til> findRecentPublicTils(Pageable pageable);
}
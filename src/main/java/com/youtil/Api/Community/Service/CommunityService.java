package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Repository.TilRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final TilRepository tilRepository;

    /**
     * 최신 TIL 10개 조회
     */
    @Transactional(readOnly = true)
    public CommunityResponseDTO.RecentTilListResponse getRecentTils() {
        // 최신 TIL 10개 조회 (공개 설정된 TIL만)
        Pageable pageable = PageRequest.of(0, 10);

        List<Til> recentTils = tilRepository.findRecentPublicTils(pageable);

        log.info("최신 TIL 조회 결과: {}개", recentTils.size());

        // DTO 변환
        List<CommunityResponseDTO.RecentTilItem> tilItems = recentTils.stream()
                .map(this::convertToRecentTilItem)
                .collect(Collectors.toList());

        return CommunityResponseDTO.RecentTilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * Til 엔티티를 RecentTilItem DTO로 변환
     */
    private CommunityResponseDTO.RecentTilItem convertToRecentTilItem(Til til) {
        return CommunityResponseDTO.RecentTilItem.builder()
                .id(til.getId())
                .userId(til.getUser().getId())
                .nickname(til.getUser().getNickname())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .title(til.getTitle())
                .category(til.getCategory())
                .tags(til.getTag())
                .recommendCount(til.getRecommendCount())
                .visitedCount(til.getVisitedCount())
                .commentsCount(til.getCommentsCount())
                .createdAt(til.getCreatedAt())
                .build();
    }
}
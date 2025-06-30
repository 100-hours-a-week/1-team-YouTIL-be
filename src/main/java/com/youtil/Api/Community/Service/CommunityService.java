package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.Til;
import com.youtil.Repository.TilRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        log.info(TilMessageCode.COMMUNITY_RECENT_TILS_FETCHED.getMessage() + ": {}개", recentTils.size());

        // DTO 변환
        List<CommunityResponseDTO.RecentTilItem> tilItems = recentTils.stream()
                .map(this::convertToRecentTilItem)
                .collect(Collectors.toList());

        return CommunityResponseDTO.RecentTilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * 커뮤니티 TIL 목록 조회 (새로 추가)
     */
    @Transactional(readOnly = true)
    public CommunityResponseDTO.CommunityTilListResponse getCommunityTils(
            CommunityRequestDTO.CommunityListRequest request) {

        log.info("커뮤니티 TIL 목록 조회 - 카테고리: {}, 페이지: {}, 크기: {}",
                request.getCategory(), request.getPage(), request.getOffset());

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getOffset() != null ? request.getOffset() : 10;
        String category = request.getCategory();

        Pageable pageable = PageRequest.of(page, size);

        List<Til> communityTils;

        if (category == null || category.trim().isEmpty() || "ENTIRE".equalsIgnoreCase(category)) {
            communityTils = tilRepository.findRecentPublicTils(pageable);
        } else {
            communityTils = tilRepository.findRecentPublicTilsByCategory(category.toUpperCase(), pageable);
        }

        // DTO 변환
        List<CommunityResponseDTO.CommunityTilItem> tilItems = communityTils.stream()
                .map(this::convertToCommunityTilItem)
                .collect(Collectors.toList());

        return CommunityResponseDTO.CommunityTilListResponse.builder()
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

    /**
     * Til 엔티티를 CommunityTilItem DTO로 변환
     */
    private CommunityResponseDTO.CommunityTilItem convertToCommunityTilItem(Til til) {
        return CommunityResponseDTO.CommunityTilItem.builder()
                .tilId(til.getId())
                .userId(til.getUser().getId())
                .useName(til.getUser().getNickname())
                .category(til.getCategory())
                .title(til.getTitle())
                .author(til.getUser().getNickname())
                .createdAt(til.getCreatedAt().toString())
                .tags(til.getTag())
                .recomment_count(til.getRecommendCount())
                .visited_count(til.getVisitedCount())
                .comments_count(til.getCommentsCount())
                .build();
    }
}

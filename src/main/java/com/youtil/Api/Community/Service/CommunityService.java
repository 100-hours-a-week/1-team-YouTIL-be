package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Converter.CommentConverter;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.CreateCommentRequest;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.CommentItem;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.GetCommentListResponseDTO;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.Comment;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.CommentRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Util.EntityValidator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final TilRepository tilRepository;
    private final EntityValidator entityValidator;
    private final CommentRepository commentRepository;

    /**
     * 최신 TIL 10개 조회
     */
    @Transactional(readOnly = true)
    public CommunityResponseDTO.RecentTilListResponse getRecentTils() {
        // 최신 TIL 10개 조회 (공개 설정된 TIL만)
        Pageable pageable = PageRequest.of(0, 10);

        List<Til> recentTils = tilRepository.findRecentPublicTils(pageable);

        log.info(TilMessageCode.COMMUNITY_RECENT_TILS_FETCHED.getMessage() + ": {}개",
                recentTils.size());

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

    @Transactional
    public CreateCommentResponse createComment(Long userId, Long tilId,
            CreateCommentRequest request) {
        // 유효성 검증들을 통합된 유틸리티로 처리
        User user = entityValidator.getValidUserOrThrow(userId);
        Til til = entityValidator.getValidTilOrThrow(tilId);
        Comment topComment = null;
        // 답글인 경우 상위 방명록 유효성 검증
        if (request.getTopCommentId() != null) {
            topComment = entityValidator.getValidCommentOrThrowException(request.getTopCommentId());
        }
        Comment comment = CommentConverter.toComment(request.getContent(), topComment, user, til);

        Comment newComment = commentRepository.save(comment);

        return CommentConverter.toCreateCommentResponse(newComment);
    }

    public GetCommentListResponseDTO getGuestbookList(Long tilId, Pageable pageable) {
        List<CommentItem> comments = commentRepository.findTopLevelCommentsWithUser(tilId,
                pageable);

        Map<Long, List<CommentItem>> repliesMap = commentRepository.findRepliesGrouped(comments);
        comments.forEach(comment ->
                comment.setReplies(repliesMap.getOrDefault(comment.getId(), List.of())));

        return CommentConverter.toGetCommentListResponseDTO(comments, pageable);
    }


}

package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Converter.CommentConverter;
import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.CreateCommentRequest;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.EditCommentRequest;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CommentItem;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentListResponseDTO;
import com.youtil.Api.Filtering.Dto.FilterRequestDto;
import com.youtil.Api.Filtering.Queue.FilterQueueProducer;
import com.youtil.Common.Enums.CommunityMessageCode;
import com.youtil.Common.Enums.Status;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.CommunityException.CommunityException.CommentNotMatchedTilException;
import com.youtil.Exception.CommunityException.CommunityException.CommentNotMatchedUserException;
import com.youtil.Model.Comment;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import com.youtil.Repository.CommentRepository;
import com.youtil.Repository.TilRecommendRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final TilRepository tilRepository;
    private final TilRecommendRepository tilRecommendRepository;
    private final UserRepository userRepository;
    private final EntityValidator entityValidator;
    private final CommentRepository commentRepository;
    private final StringRedisTemplate redisTemplate;
    private final WebClient webClient;
    private final FilterQueueProducer filterQueueProducer;

    /**
     * 최신 TIL 10개 조회
     */
    @Transactional(readOnly = true)
    public CommunityResponseDTO.RecentTilListResponse getRecentTils() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Til> recentTils = tilRepository.findRecentPublicTils(pageable);

        log.info(TilMessageCode.COMMUNITY_RECENT_TILS_FETCHED.getMessage() + ": {}개",
                recentTils.size());

        List<CommunityResponseDTO.RecentTilItem> tilItems = recentTils.stream()
                .map(this::convertToRecentTilItem)
                .collect(Collectors.toList());

        return CommunityResponseDTO.RecentTilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * 커뮤니티 TIL 목록 조회
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
        List<Til> communityTils = getTilsByCategory(category, pageable);

        List<CommunityResponseDTO.CommunityTilItem> tilItems = communityTils.stream()
                .map(this::convertToCommunityTilItem)
                .collect(Collectors.toList());

        return CommunityResponseDTO.CommunityTilListResponse.builder()
                .tils(tilItems)
                .build();
    }

    /**
     * TIL 상세 조회
     */
    @Transactional
    public CommunityResponseDTO.CommunityPostDetailResponse getTilDetail(Long tilId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당하는 유저가 존재하지 않습니다."));

        Til til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("해당하는 게시글이 존재하지 않습니다."));

        boolean liked = false;
        if (userId != null) {
            liked = tilRecommendRepository.findByTilIdAndUserId(tilId, userId).isPresent();
        }

        validateTilAccess(til);
        incrementViewCount(til, tilId);

        return convertToTilDetail(til, liked);
    }

    /**
     * TIL 좋아요/취소 토글
     */
    @Transactional
    public CommunityResponseDTO.CommunityLikeResponse toggleTilLike(Long tilId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당하는 유저가 존재하지 않습니다."));

        Til til = tilRepository.findById(tilId)
                .orElseThrow(() -> new RuntimeException("해당하는 게시글이 존재하지 않습니다."));

        validateTilAccess(til);

        Optional<TilRecommend> existingLike = tilRecommendRepository.findByTilIdAndUserId(tilId,
                userId);

        boolean isLiked;
        int newLikeCount;

        redisTemplate.opsForZSet()
                .add("changed:tils", String.valueOf(tilId), System.currentTimeMillis());

        if (existingLike.isPresent()) {
            tilRecommendRepository.delete(existingLike.get());
            newLikeCount = til.getRecommendCount() - 1;
            til.setRecommendCount(newLikeCount);
            redisTemplate.opsForValue().increment("til:" + tilId + ":like_count", -1);
            isLiked = false;
        } else {
            TilRecommend newLike = TilRecommend.builder()
                    .til(til)
                    .user(user)
                    .build();
            tilRecommendRepository.save(newLike);
            newLikeCount = til.getRecommendCount() + 1;
            til.setRecommendCount(newLikeCount);
            redisTemplate.opsForValue().increment("til:" + tilId + ":like_count", 1);
            isLiked = true;
        }

        tilRepository.save(til);

        return CommunityResponseDTO.CommunityLikeResponse.builder()
                .liked(isLiked)
                .likeCount(newLikeCount)
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
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .category(til.getCategory())
                .title(til.getTitle())
                .author(til.getUser().getNickname())
                .createdAt(til.getCreatedAt().toString())
                .tags(til.getTag())
                .recommend_count(til.getRecommendCount())
                .visited_count(til.getVisitedCount())
                .comments_count(til.getCommentsCount())
                .build();
    }

    /**
     * Til 엔티티를 CommunityPostDetailResponse DTO로 변환
     */
    private CommunityResponseDTO.CommunityPostDetailResponse convertToTilDetail(Til til,
            boolean liked) {
        return CommunityResponseDTO.CommunityPostDetailResponse.builder()
                .userId((til.getUser().getId()))
                .postId(til.getId())
                .title(til.getTitle())
                .content(til.getContent())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .author(til.getUser().getNickname())
                .tags(til.getTag())
                .createdAt(til.getCreatedAt().toString())
                .recommend_count(til.getRecommendCount())
                .visited_count(til.getVisitedCount())
                .comments_count(til.getCommentsCount())
                .liked(liked)
                .build();
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public CreateCommentResponse createComment(Long userId, Long tilId,
            CreateCommentRequest request) {
        User user = entityValidator.getValidUserOrThrow(userId);
        Til til = entityValidator.getValidTilOrThrow(tilId);
        Comment topComment = null;

        if (request.getTopCommentId() != null) {
            topComment = entityValidator.getValidCommentOrThrowException(request.getTopCommentId());
        }

        Comment comment = CommentConverter.toComment(request.getContent(), topComment, user, til);
        Comment newComment = commentRepository.save(comment);

        FilterRequestDto filterRequestDto = FilterRequestDto.builder()
                .id(newComment.getId())
                .content(newComment.getContent())
                .type("COMMENT").build();

        filterQueueProducer.enqueueFilterRequest(userId, filterRequestDto);
        redisTemplate.opsForZSet()
                .add("changed:tils", String.valueOf(tilId), System.currentTimeMillis());
        redisTemplate.opsForValue().increment("til:" + tilId + ":comment_count", 1);

        return CommentConverter.toCreateCommentResponse(newComment);
    }

    /**
     * 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public GetCommentListResponseDTO getCommentsList(Long tilId, Pageable pageable) {
        List<CommentItem> comments = commentRepository.findTopLevelCommentsWithUser(tilId,
                pageable);

        Map<Long, List<CommentItem>> repliesMap = commentRepository.findRepliesGrouped(comments);
        comments.forEach(comment ->
                comment.setReplies(repliesMap.getOrDefault(comment.getId(), List.of())));

        return CommentConverter.toGetCommentListResponseDTO(comments, pageable);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void editComment(Long tilId, Long commentId, Long userId, EditCommentRequest request) {
        User user = entityValidator.getValidUserOrThrow(userId);
        Til til = entityValidator.getValidTilOrThrow(tilId);
        Comment comment = entityValidator.getValidCommentOrThrowException(commentId);

        if (!entityValidator.isMatchedCommentAndUser(comment, user)) {
            throw new CommentNotMatchedUserException();
        }
        if (!entityValidator.isMatchedCommentAndTil(comment, til)) {
            throw new CommentNotMatchedTilException();
        }

        comment.setContent(request.getContent());
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long tilId, Long commentId, Long userId) {
        Comment comment = entityValidator.getValidCommentOrThrowException(commentId);
        User user = entityValidator.getValidUserOrThrow(userId);
        Til til = entityValidator.getValidTilOrThrow(tilId);

        if (!entityValidator.isMatchedCommentAndTil(comment, til)) {
            throw new CommentNotMatchedTilException();
        }
        if (!entityValidator.isMatchedTilAndUser(til, user)
                && !entityValidator.isMatchedCommentAndUser(comment, user)) {
            throw new CommentNotMatchedUserException();
        }

        Long commentOwnerId = comment.getUser().getId();
        Long postOwnerId = comment.getTil().getUser().getId();

        boolean hasReplies = commentRepository.existsByTopCommentId(commentId);

        redisTemplate.opsForZSet()
                .add("changed:tils", String.valueOf(tilId), System.currentTimeMillis());
        redisTemplate.opsForValue().increment("til:" + tilId + ":comment_count", -1);

        if (hasReplies) {
            comment.setStatus(Status.deactive);

            if (userId.equals(commentOwnerId)) {
                comment.setContent(CommunityMessageCode.COMMENT_DELETE_BY_USER.getMessage());
            } else if (userId.equals(postOwnerId)) {
                comment.setContent(CommunityMessageCode.COMMENT_DELETE_BY_OWNER.getMessage());
            }

            commentRepository.save(comment);
            return;
        }

        comment.setStatus(Status.deactive);
    }


    // Private helper methods
    private List<Til> getTilsByCategory(String category, Pageable pageable) {
        if (category == null || category.trim().isEmpty() || "ENTIRE".equalsIgnoreCase(category)) {
            return tilRepository.findRecentPublicTils(pageable);
        } else {
            return tilRepository.findRecentPublicTilsByCategory(category.toUpperCase(), pageable);
        }
    }

    private void validateTilAccess(Til til) {
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException("해당하는 게시글이 존재하지 않습니다.");
        }
        if (!til.getIsDisplay()) {
            throw new RuntimeException("해당하는 게시글이 존재하지 않습니다.");
        }
    }

    private void incrementViewCount(Til til, Long tilId) {
        redisTemplate.opsForZSet()
                .add("changed:tils", String.valueOf(tilId), System.currentTimeMillis());
        redisTemplate.opsForValue().increment("til:" + tilId + ":visit_count", 1);

        til.setVisitedCount(til.getVisitedCount() + 1);
        tilRepository.save(til);
    }
}

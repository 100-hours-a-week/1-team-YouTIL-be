package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.CommentItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.QComment;
import com.youtil.Model.QUser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QComment comment = QComment.comment;
    QUser user = QUser.user;

    public List<CommentItem> findTopLevelCommentsWithUser(Long tilId, Pageable pageable) {

        return queryFactory
                .select(Projections.constructor(
                        CommentItem.class,
                        comment.id,
                        comment.user.id,
                        user.nickname,
                        user.profileImageUrl,
                        comment.content,
                        comment.topComment.id,
                        comment.createdAt,
                        comment.updatedAt,
                        comment.status.eq(Status.active)
                ))
                .from(comment)
                .join(comment.user, user)
                .where(
                        comment.til.id.eq(tilId),
                        comment.topComment.isNull()
                )
                .orderBy(comment.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public Map<Long, List<CommentItem>> findRepliesGrouped(List<CommentItem> parents) {
        if (parents.isEmpty()) {
            return Map.of();
        }

        List<Long> parentIds = parents.stream().map(CommentItem::getId).toList();

        List<CommentItem> replies = queryFactory
                .select(Projections.constructor(
                        CommentItem.class,
                        comment.id,
                        comment.user.id,
                        user.nickname,
                        user.profileImageUrl,
                        comment.content,
                        comment.topComment.id,
                        comment.createdAt,
                        comment.updatedAt,
                        comment.status.eq(Status.active)))
                .from(comment)
                .join(comment.user, user)
                .where(comment.topComment.id.in(parentIds))
                .orderBy(comment.createdAt.asc())
                .fetch();

        return replies.stream()
                .collect(Collectors.groupingBy(CommentItem::getTopCommentId));
    }

}

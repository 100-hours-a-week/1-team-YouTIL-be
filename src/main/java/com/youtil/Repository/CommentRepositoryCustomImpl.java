package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
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
                .select(Projections.fields(
                        CommentItem.class,
                        comment.id.as("id"),
                        comment.user.id.as("userId"),
                        user.nickname.as("nickname"),
                        user.profileImageUrl.as("profileImageUrl"),
                        comment.content.as("content"),
                        comment.topComment.id.as("topCommentId"),
                        comment.createdAt.as("createdAt"),
                        comment.updatedAt.as("updatedAt"),
                        comment.status.eq(Status.deactive).as("deleted")
                ))
                .from(comment)
                .join(comment.user, user)
                .where(
                        comment.til.id.eq(tilId),
                        comment.topComment.isNull(),
                        comment.status.eq(Status.active)
                                .or(comment.id.in(
                                        JPAExpressions
                                                .select(comment.topComment.id)
                                                .from(comment)
                                                .where(
                                                        comment.topComment.isNotNull(),
                                                        comment.status.eq(Status.active),
                                                        comment.topComment.til.id.eq(tilId)
                                                )
                                ))
                )
                .orderBy(comment.createdAt.desc())
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
                .select(Projections.fields(
                        CommentItem.class,
                        comment.id.as("id"),
                        comment.user.id.as("userId"),
                        user.nickname,
                        user.profileImageUrl,
                        comment.content,
                        comment.topComment.id.as("topCommentId"),
                        comment.createdAt,
                        comment.updatedAt,
                        comment.status.eq(Status.deactive).as("deleted")
                ))
                .from(comment)
                .join(comment.user, user)
                .where(comment.topComment.id.in(parentIds),
                        comment.status.eq(Status.active))
                .orderBy(comment.createdAt.asc())
                .fetch();

        return replies.stream()
                .collect(Collectors.groupingBy(CommentItem::getTopCommentId));
    }

}

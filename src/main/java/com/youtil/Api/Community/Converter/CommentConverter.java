package com.youtil.Api.Community.Converter;

import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.CommentItem;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.GetCommentListResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Comment;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;

public class CommentConverter {

    public static Comment toComment(String content, Comment topComment, User user, Til til) {
        return Comment.builder()
                .til(til)
                .topComment(topComment)
                .content(content)
                .user(user)
                .status(Status.active)
                .build();
    }

    public static CreateCommentResponse toCreateCommentResponse(Comment comment) {
        return CreateCommentResponse.builder()
                .commentId(comment.getId())
                .build();
    }

    public static GetCommentListResponseDTO toGetCommentListResponseDTO(List<CommentItem> comments,
            Pageable pageable) {
        return GetCommentListResponseDTO.builder()
                .comments(comments)
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .build();
    }

}

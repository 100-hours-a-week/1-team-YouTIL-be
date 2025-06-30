package com.youtil.Api.Community.Converter;

import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Comment;
import com.youtil.Model.Til;
import com.youtil.Model.User;

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

}

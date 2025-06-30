package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityMessageCode {
    FIND_COMMENTS_SUCCESS("200", "댓글리스트가 성공적으로 조회가 되었습니다."),
    COMMENT_INACTIVATE_SUCCESS("200", "댓글삭제에 성공했습니다."),
    COMMENT_EDIT_SUCCESS("200", "댓글 수정에 성공했습니다."),
    COMMENT_DELETE_BY_USER("200", "댓글 작성자에 의해 삭제된 댓글입니다."),
    COMMENT_DELETE_BY_OWNER("200", "해당 게시물 작성자에 의해 삭제된 댓글입니다."),
    COMMENT_CREATED("201", "댓글이 성공적으로 생성되었습니다.");
    private final String code;
    private final String message;
}

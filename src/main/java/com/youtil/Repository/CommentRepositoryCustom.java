package com.youtil.Repository;


import com.youtil.Api.Community.Dto.CommunityResponseDTO.CommentItem;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface CommentRepositoryCustom {

    List<CommentItem> findTopLevelCommentsWithUser(Long tilId, Pageable pageable);

    Map<Long, List<CommentItem>> findRepliesGrouped(List<CommentItem> parents);

}

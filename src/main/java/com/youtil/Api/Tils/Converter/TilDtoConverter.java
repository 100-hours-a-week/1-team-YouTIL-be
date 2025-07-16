package com.youtil.Api.Tils.Converter;

import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO.TilRecordYearsItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TIL 관련 DTO 변환 클래스
 */
public class TilDtoConverter {

    public static TilRecordYearsItem toUserTilCountYearsItem(
            Map<Integer, List<Integer>> monthMap) {

        return TilRecordYearsItem.builder()
                .jan(monthMap.get(1))
                .feb(monthMap.get(2))
                .mar(monthMap.get(3))
                .apr(monthMap.get(4))
                .may(monthMap.get(5))
                .jun(monthMap.get(6))
                .jul(monthMap.get(7))
                .aug(monthMap.get(8))
                .sep(monthMap.get(9))
                .oct(monthMap.get(10))
                .nov(monthMap.get(11))
                .dec(monthMap.get(12))
                .build();
    }

    /**
     * CreateWithAiRequest와 AiResponse로부터 CreateAiTilRequest 생성
     */
    public static TilRequestDTO.CreateAiTilRequest toCreateAiTilRequest(
            TilRequestDTO.CreateWithAiRequest request,
            TilAiResponseDTO aiResponse) {

        List<String> tags = new ArrayList<>(aiResponse.getKeywords());

        return TilRequestDTO.CreateAiTilRequest.builder()
                .repo(String.valueOf(request.getRepositoryId()))
                .title(request.getTitle())
                .category(request.getCategory())
                .content(aiResponse.getContent())
                .tags(tags)
                .isShared(request.getIsShared())
                .build();
    }

    /**
     * Til 엔티티를 TilDetailResponse로 변환
     */
    public static TilResponseDTO.TilDetailResponse toTilDetailResponse(Til til) {
        return TilResponseDTO.TilDetailResponse.builder()
                .id(til.getId())
                .userId(til.getUser().getId())
                .nickname(til.getUser().getNickname())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .title(til.getTitle())
                .content(til.getContent())
                .category(til.getCategory())
                .tag(til.getTag())
                .isDisplay(til.getIsDisplay())
                .commitRepository(til.getCommitRepository())
                .isUploaded(til.getIsUploaded())
                .recommendCount(til.getRecommendCount())
                .visitedCount(til.getVisitedCount())
                .commentsCount(til.getCommentsCount())
                .createdAt(til.getCreatedAt())
                .updatedAt(til.getUpdatedAt())
                .build();
    }

    /**
     * 태그 리스트 처리
     */
    public static List<String> processTagList(List<String> tags, String category) {
        List<String> result = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            result.addAll(tags);
        }
        return result;
    }

    /**
     * TIL 엔티티 생성 isShared 값이 true면 isDisplay에 1을, false면 0을 저장
     */
    public static Til createTilEntity(TilRequestDTO.CreateAiTilRequest request, User user,
            List<String> tags) {
        // isShared 값에 따라 display 값 설정 (true -> 1, false -> 0)
        Boolean isDisplay = request.getIsShared() != null && request.getIsShared();

        return Til.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .tag(tags)
                .isDisplay(isDisplay) // isShared 값에 따라 true/false 설정
                .commitRepository(request.getRepo())
                .isUploaded(true) // GitHub 업로드 여부는 별도로 관리됨, 기본값은 true
                .recommendCount(0)
                .visitedCount(0)
                .commentsCount(0)
                .status(Status.active)
                .build();
    }
}

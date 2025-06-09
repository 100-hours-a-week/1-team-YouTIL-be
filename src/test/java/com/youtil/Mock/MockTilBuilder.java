package com.youtil.Mock;

import com.youtil.Common.Enums.Status;
import static com.youtil.Constants.MockTilConstants.INITIAL_COMMENTS_COUNT;
import static com.youtil.Constants.MockTilConstants.INITIAL_RECOMMEND_COUNT;
import static com.youtil.Constants.MockTilConstants.INITIAL_VISITED_COUNT;
import static com.youtil.Constants.MockTilConstants.IS_DISPLAYED;
import static com.youtil.Constants.MockTilConstants.MOCK_CATEGORY;
import static com.youtil.Constants.MockTilConstants.MOCK_CONTENT;
import static com.youtil.Constants.MockTilConstants.MOCK_TAGS;
import static com.youtil.Constants.MockTilConstants.MOCK_TIL_ID;
import static com.youtil.Constants.MockTilConstants.MOCK_TITLE;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import static com.youtil.Constants.MockTilConstants.*;

public class MockTilBuilder {

    public static Til createMockTil(User user) {
        return Til.builder()
                .id(MOCK_TIL_ID)
                .user(user)
                .status(Status.active)
                .title(MOCK_TITLE)
                .content(MOCK_CONTENT)
                .tag(MOCK_TAGS)
                .category(MOCK_CATEGORY)
                .commentsCount(INITIAL_COMMENTS_COUNT)
                .visitedCount(INITIAL_VISITED_COUNT)
                .isDisplay(IS_DISPLAYED)
                .recommendCount(INITIAL_RECOMMEND_COUNT)
                .build();
    }

    /**
     * AI 응답 Mock 데이터 생성
     */
    public static TilAiResponseDTO createDefaultAiResponse() {
        return TilAiResponseDTO.builder()
                .content(AI_RESPONSE_CONTENT)
                .keywords(AI_KEYWORDS)
                .build();
    }

    /**
     * 빈 AI 응답 Mock 데이터 생성
     */
    public static TilAiResponseDTO createEmptyAiResponse() {
        return TilAiResponseDTO.builder()
                .content("")
                .keywords(java.util.Collections.emptyList())
                .build();
    }

}

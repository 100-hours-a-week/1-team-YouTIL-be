package com.youtil.Api.Interview.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class InterviewResponseDTO {

    @Getter
    @Builder
    public static class CreateInterviewAIResponse {

        private String summary;
        private List<InterviewQuestionResponse> content;
    }

    @Getter
    @Builder
    public static class InterviewQuestionResponse {

        private String question;
        private String answer;
    }

    @Getter
    @Builder
    public static class GetInterviewsResponse {

        private List<InterviewsItem> interviews;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class InterviewsItem {

        private long id;
        private String title;
        private String level;
        private OffsetDateTime createdAt;
    }

    @Getter
    @Builder
    public static class GetInterviewResponse {

        private long id;
        private String title;
        private String level;
        private OffsetDateTime createdAt;
        private List<GetInterviewQuestionItem> questions;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GetInterviewQuestionItem {

        private long questionId;
        private String question;
        private String answer;
    }
}

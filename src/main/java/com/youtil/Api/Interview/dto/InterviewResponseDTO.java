package com.youtil.Api.Interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class InterviewResponseDTO {


    @Getter
    @Builder
    public static class CreateInterviewResponseDTO {

        private Long interviewId;
    }

    @Builder
    @Getter
    public static class CreateRequestId {

        private String requestId;
    }

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

    @Getter
    @Builder
    public static class GetInterviewCountResponse {

        @Schema(description = "연도", example = "2025")
        private int year;
        @Schema(description = "면접 질문 작성 정보")
        private InterviewRecordYearsItem interviews;
    }

    @Getter
    @Builder
    public static class InterviewRecordYearsItem {

        @Schema(description = "1월", example = "[0,0,0,0]")
        private List<Integer> jan;
        @Schema(description = "2월", example = "[0,0,0,0]")
        private List<Integer> feb;
        @Schema(description = "3월", example = "[0,0,0,0]")
        private List<Integer> mar;
        @Schema(description = "4월", example = "[0,0,0,0]")
        private List<Integer> apr;
        @Schema(description = "5월", example = "[0,0,0,0]")
        private List<Integer> may;
        @Schema(description = "6월", example = "[0,0,0,0]")
        private List<Integer> jun;
        @Schema(description = "7월", example = "[0,0,0,0]")
        private List<Integer> jul;
        @Schema(description = "8월", example = "[0,0,0,0]")
        private List<Integer> aug;
        @Schema(description = "9월", example = "[0,0,0,0]")
        private List<Integer> sep;
        @Schema(description = "10월", example = "[0,0,0,0]")
        private List<Integer> oct;
        @Schema(description = "11월", example = "[0,0,0,0]")
        private List<Integer> nov;
        @Schema(description = "12월", example = "[0,0,0,0]")
        private List<Integer> dec;

    }
}

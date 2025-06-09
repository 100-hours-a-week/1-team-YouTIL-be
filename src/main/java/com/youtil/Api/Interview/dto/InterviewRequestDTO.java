package com.youtil.Api.Interview.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class InterviewRequestDTO {

    @Getter
    @Builder
    public static class CreateInterviewRequest {

        private long tilId;
        private int level;

    }

    @Getter
    @Builder
    public static class CreateInterviewAIRequest {

        private String title;
        private String til;
        private int level;
        private String email;
        private List<String> keywords;
        private String category;
    }

    @Getter
    @Builder
    public static class InactiveInterviewRequest {

        private List<Long> interviewIds;
    }
}

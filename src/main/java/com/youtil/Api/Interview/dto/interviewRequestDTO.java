package com.youtil.Api.Interview.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class interviewRequestDTO {

    @Getter
    @Setter
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
    }
}

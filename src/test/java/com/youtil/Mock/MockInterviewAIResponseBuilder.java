package com.youtil.Mock;

import com.youtil.Api.Interview.dto.InterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewAIResponse;
import java.util.List;

public class MockInterviewAIResponseBuilder {

    public static CreateInterviewAIResponse createInterviewAIResponse() {
        return CreateInterviewAIResponse.builder()
                .summary("요약 내용입니다.")
                .content(List.of(
                        InterviewResponseDTO.InterviewQuestionResponse.builder().question("Q1")
                                .answer("A1").build(),
                        InterviewResponseDTO.InterviewQuestionResponse.builder().question("Q2")
                                .answer("A2").build()
                ))
                .build();
    }

}

package com.youtil.Api.Interview.Converter;

import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewQuestionItem;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewsResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewRecordYearsItem;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewsItem;
import com.youtil.Common.Enums.Level;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Interview;
import com.youtil.Model.InterviewQuestion;
import com.youtil.Model.Til;
import java.util.List;
import java.util.Map;

public class InterviewConverter {

    public static Interview toInterview(Til til, String title, Level level) {
        return Interview.builder()
                .til(til)
                .title(title)
                .status(Status.active)
                .level(level)
                .build();
    }

    public static InterviewQuestion toInterviewQuestion(Interview interview, String question,
            String answer) {
        return InterviewQuestion.builder()
                .interview(interview)
                .question(question)
                .answer(answer)
                .build();
    }

    public static GetInterviewsResponse toGetInterviewsResponse(List<InterviewsItem> interviews) {
        return GetInterviewsResponse.builder()
                .interviews(interviews)
                .build();
    }

    public static GetInterviewResponse toGetInterviewResponse(Interview interview,
            List<GetInterviewQuestionItem> getInterviewQuestionItem) {
        return GetInterviewResponse.builder()
                .id(interview.getId())
                .title(interview.getTitle())
                .level(interview.getLevel().toString())
                .questions(getInterviewQuestionItem)
                .createdAt(interview.getCreatedAt())
                .build();
    }

    public static InterviewRecordYearsItem toInterviewRecordYearsItem(
            Map<Integer, List<Integer>> monthMap) {

        return InterviewRecordYearsItem.builder()
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

}

package com.youtil.Repository;

import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewQuestionItem;
import java.util.List;

public interface InterviewQuestionRepositoryCustom {

    List<GetInterviewQuestionItem> findAllInterviewQuestionsByInterview(
            long interviewId);

}

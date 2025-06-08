package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewQuestionItem;
import com.youtil.Model.QInterviewQuestion;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewQuestionRepositoryCustomImpl implements InterviewQuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public List<GetInterviewQuestionItem> findAllInterviewQuestionsByInterview(
            long interviewId) {
        QInterviewQuestion question = QInterviewQuestion.interviewQuestion;

        return queryFactory.select(Projections.constructor(GetInterviewQuestionItem.class,
                        question.id,
                        question.question,
                        question.answer)
                ).from(question)
                .where(question.interview.id.eq(interviewId)).fetch();

    }

}

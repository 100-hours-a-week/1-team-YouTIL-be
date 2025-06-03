package com.youtil.Repository;


import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewsItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.QInterview;
import com.youtil.Model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewRepositoryCustomImpl implements InterviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public List<InterviewsItem> findAllUserInterviewByDate(User user, Pageable pageable,
            LocalDate date) {
        QInterview interview = QInterview.interview;
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        OffsetDateTime start = startOfDay.atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endOfDay.atOffset(ZoneOffset.UTC);
        return queryFactory
                .select(Projections.constructor(InterviewsItem.class,
                        interview.id,
                        interview.title,
                        interview.level.stringValue(),
                        interview.createdAt))
                .from(interview)
                .where(
                        interview.til.user.id.eq(user.getId()),
                        interview.status.eq(Status.active),
                        interview.createdAt.goe(start),
                        interview.createdAt.lt(end)
                )
                .orderBy(interview.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }


}

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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
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

        ZoneId KST = ZoneId.of("Asia/Seoul");

        // KST 기준 날짜 범위
        LocalDateTime startKst = date.atStartOfDay(); // 00:00
        LocalDateTime endKst = date.plusDays(1).atStartOfDay(); // 다음 날 00:00

        // KST → UTC 변환
        OffsetDateTime startUtc = startKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toOffsetDateTime();
        OffsetDateTime endUtc = endKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toOffsetDateTime();

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
                        interview.createdAt.goe(startUtc),
                        interview.createdAt.lt(endUtc)
                )
                .orderBy(interview.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<LocalDate> findInterviewedDatesByUserAndYear(Long userId, int year) {
        QInterview interview = QInterview.interview;

        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDateTime startKst = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endKst = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        // KST → UTC 변환
        LocalDateTime startUtc = startKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime endUtc = endKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        return queryFactory
                .select(interview.createdAt)
                .from(interview)
                .where(
                        interview.til.user.id.eq(userId),
                        interview.status.eq(Status.active),
                        interview.createdAt.between(startUtc.atOffset(ZoneOffset.UTC),
                                endUtc.atOffset(ZoneOffset.UTC))
                )
                .fetch()
                .stream()
                .map(offsetDateTime -> offsetDateTime.atZoneSameInstant(KST)
                        .toLocalDate()) // UTC → KST 기준 날짜
                .distinct()
                .collect(Collectors.toList());
    }


}

package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.QTil;
import com.youtil.Model.QUser;
import com.youtil.Model.Til;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TilRepositoryCustomImpl implements TilRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자의 특정 연도 TIL 목록 조회
     */
    @Override
    public List<Til> findAllByUserIdAndYear(long userId, int year) {
        QTil til = QTil.til;

        return queryFactory
                .selectFrom(til)
                .where(
                        til.user.id.eq(userId),
                        til.createdAt.year().eq(year),
                        til.status.eq(Status.active)
                )
                .orderBy(til.createdAt.desc())
                .fetch();
    }

    @Override
    public List<LocalDate> findTilledDatesByUserAndYear(Long userId, int year) {
        QTil til = QTil.til;

        return queryFactory
                .select(til.createdAt)
                .from(til)
                .where(
                        til.user.id.eq(userId),
                        til.createdAt.year().eq(year),
                        til.status.eq(Status.active)
                )
                .fetch()
                .stream()
                .map(createdAt -> createdAt.toLocalDate()) // OffsetDateTime or LocalDateTime에 따라
                .distinct()
                .collect(Collectors.toList());

    }

    /**
     * 특정 사용자의 TIL 목록 조회 (페이징)
     */
    @Override
    public List<TilListItem> findUserTils(Long userId, Pageable pageable) {
        QTil til = QTil.til;
        QUser user = QUser.user;

        return queryFactory
                .select(Projections.constructor(TilListItem.class,
                        til.user.id,
                        til.user.nickname,
                        til.user.profileImageUrl,
                        til.id,
                        til.title,
                        til.tag,
                        til.createdAt
                ))
                .from(til)
                .join(til.user, user)
                .where(
                        til.user.id.eq(userId),
                        til.status.eq(Status.active)
                        // isDisplay 조건 제거: 모든 상태(공개/비공개)의 TIL 조회
                )
                .orderBy(til.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 특정 날짜 범위 내의 사용자 TIL 목록 조회 (페이징)
     */
    @Override
    public List<TilListItem> findUserTilsByDateRange(
            Long userId, LocalDateTime startDateTimeKst, LocalDateTime endDateTimeKst,
            Pageable pageable) {

        QTil til = QTil.til;
        QUser user = QUser.user;

        // KST → UTC 변환
        ZoneId KST = ZoneId.of("Asia/Seoul");

        LocalDateTime startUtc = startDateTimeKst != null
                ? startDateTimeKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : null;

        LocalDateTime endUtc = endDateTimeKst != null
                ? endDateTimeKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : null;

        BooleanExpression dateCondition = til.createdAt.isNotNull();

        if (startUtc != null) {
            dateCondition = dateCondition.and(til.createdAt.goe(startUtc.atOffset(ZoneOffset.UTC)));
        }

        if (endUtc != null) {
            dateCondition = dateCondition.and(til.createdAt.loe(endUtc.atOffset(ZoneOffset.UTC)));
        }

        return queryFactory
                .select(Projections.constructor(TilListItem.class,
                        til.user.id,
                        til.user.nickname,
                        til.user.profileImageUrl,
                        til.id,
                        til.title,
                        til.tag,
                        til.createdAt
                ))
                .from(til)
                .join(til.user, user)
                .where(
                        til.user.id.eq(userId),
                        til.status.eq(Status.active),
                        dateCondition
                )
                .orderBy(til.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 최신 공개 TIL 목록 조회 (페이징) 활성화 상태(active)이고, 공개 설정(isDisplay=true)된 TIL만 조회
     */
    @Override
    public List<Til> findRecentPublicTils(Pageable pageable) {
        QTil til = QTil.til;
        QUser user = QUser.user;

        return queryFactory
                .selectFrom(til)
                .join(til.user, user).fetchJoin() // N+1 문제 방지를 위한 fetchJoin
                .where(
                        til.status.eq(Status.active),
                        til.isDisplay.eq(true)
                )
                .orderBy(til.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}

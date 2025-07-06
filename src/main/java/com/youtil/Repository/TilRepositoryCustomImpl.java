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

        // 1. KST 기준으로 해당 연도의 시작과 끝 날짜 설정
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDateTime startOfYearKST = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endOfYearKST = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        // 2. KST → UTC 변환 (createdAt은 UTC로 저장되었다고 가정)
        LocalDateTime startUtc = startOfYearKST.atZone(KST)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime endUtc = endOfYearKST.atZone(KST)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        return queryFactory
                .selectFrom(til)
                .where(
                        til.user.id.eq(userId),
                        til.status.eq(Status.active),
                        til.createdAt.between(
                                startUtc.atOffset(ZoneOffset.UTC),
                                endUtc.atOffset(ZoneOffset.UTC)
                        )
                )
                .orderBy(til.createdAt.desc())
                .fetch();
    }

    @Override
    public List<LocalDate> findTilledDatesByUserAndYear(Long userId, int year) {
        QTil til = QTil.til;

        // 1. year의 첫날과 마지막 날의 시작/끝을 KST 기준으로 지정
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDateTime startOfYearKST = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endOfYearKST = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        // 2. UTC로 변환
        LocalDateTime startUtc = startOfYearKST.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        LocalDateTime endUtc = endOfYearKST.atZone(KST).withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        return queryFactory
                .select(til.createdAt)
                .from(til)
                .where(
                        til.user.id.eq(userId),
                        til.status.eq(Status.active),
                        til.createdAt.between(startUtc.atOffset(ZoneOffset.UTC),
                                endUtc.atOffset(ZoneOffset.UTC))
                )
                .fetch()
                .stream()
                .map(offsetDateTime -> offsetDateTime.atZoneSameInstant(KST)
                        .toLocalDate()) // UTC → KST 날짜로 변환
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
                        til.createdAt,
                        til.visitedCount,
                        til.recommendCount
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
                        til.createdAt,
                        til.visitedCount,
                        til.recommendCount
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

    /**
     * 카테고리별 공개 TIL을 최신순으로 조회
     */
    @Override
    public List<Til> findRecentPublicTilsByCategory(String category, Pageable pageable) {
        QTil til = QTil.til;
        QUser user = QUser.user;

        return queryFactory
                .selectFrom(til)
                .join(til.user, user).fetchJoin()
                .where(
                        til.status.eq(Status.active),
                        til.isDisplay.eq(true),
                        til.category.equalsIgnoreCase(category)
                )
                .orderBy(til.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    //TIL 카운트 수 업데이트
    @Override
    public void updateCounts(Long tilId, int likes, int comments, int views) {
        QTil til = QTil.til;
        queryFactory.update(til)
                .set(til.recommendCount, likes)
                .set(til.commentsCount, comments)
                .set(til.visitedCount, views)
                .where(til.id.eq(tilId))
                .execute();
    }
}

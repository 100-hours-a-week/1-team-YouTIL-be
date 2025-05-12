package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.QTil;
import com.youtil.Model.QUser;
import com.youtil.Model.Til;
import java.time.LocalDateTime;
import java.util.List;
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
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime,
            Pageable pageable) {
        QTil til = QTil.til;
        QUser user = QUser.user;

        // 날짜 범위 조건을 BooleanExpression으로 생성
        BooleanExpression dateCondition = til.createdAt.isNotNull(); // 기본 조건

        // startDateTime이 있으면 조건 추가
        if (startDateTime != null) {
            dateCondition = dateCondition.and(
                    til.createdAt.year().goe(startDateTime.getYear())
                            .and(til.createdAt.month().goe(startDateTime.getMonthValue()))
                            .and(til.createdAt.dayOfMonth().goe(startDateTime.getDayOfMonth()))
            );
        }

        // endDateTime이 있으면 조건 추가
        if (endDateTime != null) {
            dateCondition = dateCondition.and(
                    til.createdAt.year().loe(endDateTime.getYear())
                            .and(til.createdAt.month().loe(endDateTime.getMonthValue()))
                            .and(til.createdAt.dayOfMonth().loe(endDateTime.getDayOfMonth()))
            );
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
                        // isDisplay 조건 제거: 모든 상태(공개/비공개)의 TIL 조회
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
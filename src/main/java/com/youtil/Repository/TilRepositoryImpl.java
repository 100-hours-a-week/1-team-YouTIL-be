package com.youtil.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import static com.youtil.Model.QTil.til;
import static com.youtil.Model.QUser.user;
import com.youtil.Model.Til;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class TilRepositoryImpl implements TilRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Til> findAllByUserIdAndYear(long userId, int year) {
        return queryFactory.selectFrom(til)
                .where(
                        til.user.id.eq(userId),
                        til.createdAt.year().eq(year)
                )
                .fetch();

    }

    @Override
    public List<TilListItem> findUserTils(Long userId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(TilListItem.class,
                        til.user.id,
                        til.user.nickname,
                        til.user.profileImageUrl,
                        til.id,
                        til.title,
                        til.tag,                          // ✅ 이미 List<String> 타입
                        til.createdAt.stringValue()
                ))
                .from(til)
                .join(til.user, user)
                .where(
                        til.user.id.eq(userId),
                        til.status.eq(Status.active),
                        til.isDisplay.eq(true)// Til이 활성 상태일 때만
                )
                .orderBy(til.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}

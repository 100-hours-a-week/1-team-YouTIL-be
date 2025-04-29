package com.youtil.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import static com.youtil.Model.QTil.til;
import com.youtil.Model.Til;
import java.util.List;
import lombok.RequiredArgsConstructor;

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
}

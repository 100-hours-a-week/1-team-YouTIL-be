package com.youtil.Model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTilRecommend is a Querydsl query type for TilRecommend
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTilRecommend extends EntityPathBase<TilRecommend> {

    private static final long serialVersionUID = -1104552909L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTilRecommend tilRecommend = new QTilRecommend("tilRecommend");

    public final QBaseTime _super = new QBaseTime(this);

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QTil til;

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> updatedAt = _super.updatedAt;

    public final QUser user;

    public QTilRecommend(String variable) {
        this(TilRecommend.class, forVariable(variable), INITS);
    }

    public QTilRecommend(Path<? extends TilRecommend> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTilRecommend(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTilRecommend(PathMetadata metadata, PathInits inits) {
        this(TilRecommend.class, metadata, inits);
    }

    public QTilRecommend(Class<? extends TilRecommend> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.til = inits.isInitialized("til") ? new QTil(forProperty("til"), inits.get("til")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}


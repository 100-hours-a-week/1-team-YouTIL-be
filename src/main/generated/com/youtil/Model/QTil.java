package com.youtil.Model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTil is a Querydsl query type for Til
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTil extends EntityPathBase<Til> {

    private static final long serialVersionUID = -1342333303L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTil til = new QTil("til");

    public final QBaseTime _super = new QBaseTime(this);

    public final StringPath category = createString("category");

    public final NumberPath<Integer> commentsCount = createNumber("commentsCount", Integer.class);

    public final StringPath commitRepository = createString("commitRepository");

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<Interview, QInterview> interviews = this.<Interview, QInterview>createList("interviews", Interview.class, QInterview.class, PathInits.DIRECT2);

    public final BooleanPath isDisplay = createBoolean("isDisplay");

    public final BooleanPath isUploaded = createBoolean("isUploaded");

    public final NumberPath<Integer> recommendCount = createNumber("recommendCount", Integer.class);

    public final EnumPath<com.youtil.Common.Enums.Status> status = createEnum("status", com.youtil.Common.Enums.Status.class);

    public final StringPath tag = createString("tag");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> updatedAt = _super.updatedAt;

    public final QUser user;

    public final NumberPath<Integer> visitedCount = createNumber("visitedCount", Integer.class);

    public QTil(String variable) {
        this(Til.class, forVariable(variable), INITS);
    }

    public QTil(Path<? extends Til> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTil(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTil(PathMetadata metadata, PathInits inits) {
        this(Til.class, metadata, inits);
    }

    public QTil(Class<? extends Til> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}


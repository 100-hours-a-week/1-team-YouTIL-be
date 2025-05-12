package com.youtil.Model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGuestbook is a Querydsl query type for Guestbook
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGuestbook extends EntityPathBase<Guestbook> {

    private static final long serialVersionUID = 362536243L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGuestbook guestbook = new QGuestbook("guestbook");

    public final QBaseTime _super = new QBaseTime(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final QUser guest;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QUser owner;

    public final EnumPath<com.youtil.Common.Enums.Status> status = createEnum("status", com.youtil.Common.Enums.Status.class);

    public final QGuestbook topGuestbook;

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> updatedAt = _super.updatedAt;

    public QGuestbook(String variable) {
        this(Guestbook.class, forVariable(variable), INITS);
    }

    public QGuestbook(Path<? extends Guestbook> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGuestbook(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGuestbook(PathMetadata metadata, PathInits inits) {
        this(Guestbook.class, metadata, inits);
    }

    public QGuestbook(Class<? extends Guestbook> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guest = inits.isInitialized("guest") ? new QUser(forProperty("guest")) : null;
        this.owner = inits.isInitialized("owner") ? new QUser(forProperty("owner")) : null;
        this.topGuestbook = inits.isInitialized("topGuestbook") ? new QGuestbook(forProperty("topGuestbook"), inits.get("topGuestbook")) : null;
    }

}


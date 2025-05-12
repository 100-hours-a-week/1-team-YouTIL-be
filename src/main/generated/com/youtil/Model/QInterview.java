package com.youtil.Model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInterview is a Querydsl query type for Interview
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInterview extends EntityPathBase<Interview> {

    private static final long serialVersionUID = 1753412083L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInterview interview = new QInterview("interview");

    public final QBaseTime _super = new QBaseTime(this);

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.youtil.Common.Enums.Level> level = createEnum("level", com.youtil.Common.Enums.Level.class);

    public final ListPath<InterviewQuestion, QInterviewQuestion> questions = this.<InterviewQuestion, QInterviewQuestion>createList("questions", InterviewQuestion.class, QInterviewQuestion.class, PathInits.DIRECT2);

    public final EnumPath<com.youtil.Common.Enums.Status> status = createEnum("status", com.youtil.Common.Enums.Status.class);

    public final QTil til;

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.OffsetDateTime> updatedAt = _super.updatedAt;

    public QInterview(String variable) {
        this(Interview.class, forVariable(variable), INITS);
    }

    public QInterview(Path<? extends Interview> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInterview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInterview(PathMetadata metadata, PathInits inits) {
        this(Interview.class, metadata, inits);
    }

    public QInterview(Class<? extends Interview> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.til = inits.isInitialized("til") ? new QTil(forProperty("til"), inits.get("til")) : null;
    }

}


package com.youtil.Model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInterviewQuestion is a Querydsl query type for InterviewQuestion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInterviewQuestion extends EntityPathBase<InterviewQuestion> {

    private static final long serialVersionUID = -1370231047L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInterviewQuestion interviewQuestion = new QInterviewQuestion("interviewQuestion");

    public final StringPath answer = createString("answer");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QInterview interview;

    public final StringPath question = createString("question");

    public QInterviewQuestion(String variable) {
        this(InterviewQuestion.class, forVariable(variable), INITS);
    }

    public QInterviewQuestion(Path<? extends InterviewQuestion> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInterviewQuestion(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInterviewQuestion(PathMetadata metadata, PathInits inits) {
        this(InterviewQuestion.class, metadata, inits);
    }

    public QInterviewQuestion(Class<? extends InterviewQuestion> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.interview = inits.isInitialized("interview") ? new QInterview(forProperty("interview"), inits.get("interview")) : null;
    }

}


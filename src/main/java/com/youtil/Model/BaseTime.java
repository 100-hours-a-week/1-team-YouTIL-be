package com.youtil.Model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTime {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private final OffsetDateTime createdAt = ZonedDateTime.now(SEOUL_ZONE).toOffsetDateTime();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = ZonedDateTime.now(SEOUL_ZONE).toOffsetDateTime();
}

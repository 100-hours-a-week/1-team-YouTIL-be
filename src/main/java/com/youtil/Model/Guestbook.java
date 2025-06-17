package com.youtil.Model;

import com.youtil.Common.Enums.GuestbookStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "guestbook")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guestbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    @Column(name = "top_gusestbook_id") // ERD의 컬럼명 그대로 유지
    private Long topGuestbookId;

    @Column(name = "content", nullable = false, length = GuestbookStatus.MAX_CONTENT_LENGTH)
    private String content;

    // AttributeConverter 사용으로 변경 - DB의 "active"/"deactive"와 enum 매핑
    @Convert(converter = GuestbookStatus.GuestbookStatusConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private GuestbookStatus status = GuestbookStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", insertable = false, updatable = false)
    private User guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top_gusestbook_id", insertable = false, updatable = false) // ERD 컬럼명 맞춤
    private Guestbook parentGuestbook;

    @OneToMany(mappedBy = "parentGuestbook", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Guestbook> replies = new ArrayList<>();

    // 소프트 삭제를 위한 메서드 (기존 방식 - 완전 삭제)
    public void softDelete() {
        this.status = GuestbookStatus.DEACTIVE;
        this.deletedAt = OffsetDateTime.now();
    }

    // 내용만 삭제 처리하는 메서드 (대댓글이 있는 경우)
    public void markAsDeleted(String deletionMessage) {
        this.content = deletionMessage;
        this.status = GuestbookStatus.ACTIVE; // 여전히 ACTIVE 상태
    }

    // 활성 상태 확인 메서드
    public boolean isActive() {
        return this.status == GuestbookStatus.ACTIVE;
    }

    // 답글인지 확인하는 메서드
    public boolean isReply() {
        return this.topGuestbookId != null;
    }

    // 최상위 방명록인지 확인하는 메서드
    public boolean isTopLevel() {
        return this.topGuestbookId == null;
    }

    // 삭제된 댓글인지 확인하는 메서드
    public boolean isDeleted() {
        return GuestbookStatus.DELETED_COMMENT_MESSAGE.equals(this.content);
    }

    // 대댓글이 있는지 확인하는 메서드
    public boolean hasActiveReplies() {
        return this.replies != null &&
                this.replies.stream().anyMatch(reply -> reply.isActive() && !reply.isDeleted());
    }
}

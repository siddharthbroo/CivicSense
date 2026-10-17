package com.civicsense.complaint.entity.community;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "complaint_comments",
        indexes = {
                @Index(name = "idx_complaint_comment_complaint", columnList = "complaint_id"),
                @Index(name = "idx_complaint_comment_user", columnList = "user_id"),
                @Index(name = "idx_complaint_comment_created_at", columnList = "created_at"),
                @Index(name = "idx_complaint_comment_complaint_created", columnList = "complaint_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ComplaintComment(Complaint complaint, User user, String content) {
        this.complaint = complaint;
        this.user = user;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


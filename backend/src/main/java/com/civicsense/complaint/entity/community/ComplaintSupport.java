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
        name = "complaint_supports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_complaint_support_user",
                        columnNames = {"complaint_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_complaint_support_complaint", columnList = "complaint_id"),
                @Index(name = "idx_complaint_support_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ComplaintSupport(Complaint complaint, User user) {
        this.complaint = complaint;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


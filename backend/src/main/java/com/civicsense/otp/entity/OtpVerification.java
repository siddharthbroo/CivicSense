package com.civicsense.otp.entity;

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
        name = "otp_verifications",
        indexes = {
                @Index(
                        name = "idx_otp_mobile_purpose_status",
                        columnList = "mobile_number, purpose, status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * User may not exist during registration.
     * It will be linked later after successful OTP verification
     * and user creation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /*
     * NOTE: identity_verification_id column is intentionally removed from this
     * entity. The column may still exist in the database (ddl-auto=update will
     * not drop it). It can be removed via a manual migration if desired.
     */

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    /*
     * Never store the actual OTP.
     * Only the hashed value is stored.
     */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status = OtpStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
package com.civicsense.officer.entity;

import com.civicsense.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the official profile of an authorized municipal officer.
 *
 * <p>Reuses the existing {@link User} entity as the underlying authentication principal
 * and identity record, while encapsulating municipal employment information, department
 * mapping, and verification lifecycle.
 *
 * <p>Enforces 1:1 relationship between a User and their OfficerProfile.
 * Employee codes are scoped to the specific municipal corporation.
 */
@Entity
@Table(name = "officer_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_officer_user", columnNames = {"user_id"}),
        @UniqueConstraint(name = "uk_officer_muni_code", columnNames = {"municipality", "employee_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfficerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 150)
    private String municipality = "Municipal Corporation of Delhi (MCD)";

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "government_email", length = 150)
    private String governmentEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private OfficerVerificationStatus verificationStatus = OfficerVerificationStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (verificationStatus == null) {
            verificationStatus = OfficerVerificationStatus.PENDING_VERIFICATION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public OfficerProfile(
            User user,
            String employeeCode,
            String municipality,
            String designation,
            String department,
            String governmentEmail
    ) {
        this.user = user;
        this.employeeCode = employeeCode;
        this.municipality = (municipality != null && !municipality.isBlank())
                ? municipality
                : "Municipal Corporation of Delhi (MCD)";
        this.designation = designation;
        this.department = department;
        this.governmentEmail = governmentEmail;
        this.verificationStatus = OfficerVerificationStatus.PENDING_VERIFICATION;
        this.active = true;
    }

    /**
     * Checks if this officer is operationally authorized to access officer queues and resolve complaints.
     */
    public boolean isOperationallyAuthorized() {
        return active && verificationStatus == OfficerVerificationStatus.VERIFIED;
    }
}


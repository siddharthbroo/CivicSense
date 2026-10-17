package com.civicsense.complaint.entity.risk;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private Complaint complaint;

    @Column(nullable = false)
    private double severityScore;

    @Column(nullable = false)
    private double corroborationScore;

    @Column(nullable = false)
    private double locationRiskScore;

    @Column(nullable = false)
    private double infrastructureRiskScore;

    @Column(nullable = false)
    private double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintPriority priority;

    /**
     * True if field/officer verification is required before automated resolution.
     * Evaluated by VerificationGateService after M5 risk assessment.
     */
    @Column(name = "verification_required")
    private Boolean verificationRequired = false;

    /**
     * Explainable reason if verification is required (e.g. HIGH_SEVERITY_LOW_EVIDENCE, SUSPICIOUS_FLAGS, NORMAL_FLOW).
     */
    @Column(name = "verification_reason", length = 500)
    private String verificationReason;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        computedAt = LocalDateTime.now();
    }
}
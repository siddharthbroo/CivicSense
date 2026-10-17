package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.RiskAssessmentRepository;
import com.civicsense.complaint.service.community.CitizenCorroborationService;
import com.civicsense.complaint.service.verification.VerificationDecision;
import com.civicsense.complaint.service.verification.VerificationGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskAssessmentServiceImpl
        implements RiskAssessmentService {

    private static final double SEVERITY_WEIGHT = 0.35;
    private static final double CORROBORATION_WEIGHT = 0.25;
    private static final double LOCATION_RISK_WEIGHT = 0.20;
    private static final double INFRASTRUCTURE_RISK_WEIGHT = 0.20;

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final LocationRiskService locationRiskService;
    private final InfrastructureRiskService infrastructureRiskService;
    private final VerificationGateService verificationGateService;
    private final ComplaintImageRepository complaintImageRepository;
    private final CitizenCorroborationService citizenCorroborationService;

    @Override
    public RiskAssessment assess(
            Complaint complaint,
            ComplaintAnalysis analysis
    ) {

        RiskAssessment assessment = complaint.getId() == null
                ? new RiskAssessment()
                : riskAssessmentRepository.findByComplaintId(complaint.getId())
                        .orElseGet(RiskAssessment::new);

        assessment.setComplaint(complaint);
        assessment.setComputedAt(LocalDateTime.now());

        // 1. Severity Risk
        double severityScore =
                calculateSeverityScore(
                        analysis.getSeverity()
                );

        assessment.setSeverityScore(
                severityScore
        );

        // 2. Corroboration Risk
        double corroborationScore =
                calculateCorroborationScore(
                        complaint
                );

        assessment.setCorroborationScore(
                corroborationScore
        );

        // 3. Location Risk
        double locationRiskScore =
                locationRiskService.calculateLocationRisk(
                        complaint
                );

        assessment.setLocationRiskScore(
                locationRiskScore
        );

        // 4. Infrastructure Risk
        double infrastructureRiskScore =
                infrastructureRiskService
                        .calculateInfrastructureRisk(
                                complaint
                        );

        assessment.setInfrastructureRiskScore(
                infrastructureRiskScore
        );

        // 5. Calculate final weighted risk score
        double riskScore =
                calculateFinalRiskScore(
                        severityScore,
                        corroborationScore,
                        locationRiskScore,
                        infrastructureRiskScore
                );

        assessment.setRiskScore(
                riskScore
        );

        // 6. Determine priority (UNTOUCHED: priority is calculated strictly by risk score)
        ComplaintPriority priority =
                determinePriority(riskScore);

        assessment.setPriority(priority);

        // 7. Verification Gate: exceptional routing check
        // Rule: High-severity/risk + low evidence, or suspicious flags -> verificationRequired = true
        // Priority is preserved and never downgraded.
        ComplaintImage complaintImage = complaintImageRepository
                .findTopByComplaintIdOrderByCreatedAtDesc(complaint.getId())
                .orElse(null);

        VerificationDecision decision = verificationGateService.evaluate(
                analysis,
                assessment,
                complaintImage
        );

        assessment.setVerificationRequired(decision.verificationRequired());
        assessment.setVerificationReason(decision.reason());

        // 8. Save only after all required fields are calculated
        RiskAssessment savedAssessment =
                riskAssessmentRepository.save(
                        assessment
                );

        System.out.println("===== RISK ASSESSMENT =====");
        System.out.println("Severity Risk         = " + severityScore);
        System.out.println("Corroboration Risk    = " + corroborationScore);
        System.out.println("Location Risk         = " + locationRiskScore);
        System.out.println("Infrastructure Risk   = " + infrastructureRiskScore);
        System.out.println("Final Risk Score      = " + riskScore);
        System.out.println("Priority              = " + priority);
        System.out.println("Verification Required = " + decision.verificationRequired());
        System.out.println("Verification Reason   = " + decision.reason());
        System.out.println("===========================");

        return savedAssessment;
    }

    private double calculateSeverityScore(
            ComplaintSeverity severity
    ) {

        if (severity == null) {
            return 0.0;
        }

        return switch (severity) {
            case LOW -> 20.0;
            case MEDIUM -> 50.0;
            case HIGH -> 75.0;
            case CRITICAL -> 100.0;
        };
    }

    private double calculateCorroborationScore(
            Complaint complaint
    ) {

        return citizenCorroborationService
                .analyzeComplaintCorroboration(complaint)
                .corroborationScore();
    }

    private double calculateFinalRiskScore(
            double severityScore,
            double corroborationScore,
            double locationRiskScore,
            double infrastructureRiskScore
    ) {

        return
                (severityScore * SEVERITY_WEIGHT)
                        + (corroborationScore * CORROBORATION_WEIGHT)
                        + (locationRiskScore * LOCATION_RISK_WEIGHT)
                        + (infrastructureRiskScore * INFRASTRUCTURE_RISK_WEIGHT);
    }

    private ComplaintPriority determinePriority(
            double riskScore
    ) {

        if (riskScore < 25.0) {
            return ComplaintPriority.LOW;
        }

        if (riskScore < 50.0) {
            return ComplaintPriority.MEDIUM;
        }

        if (riskScore < 75.0) {
            return ComplaintPriority.HIGH;
        }

        return ComplaintPriority.CRITICAL;
    }
}

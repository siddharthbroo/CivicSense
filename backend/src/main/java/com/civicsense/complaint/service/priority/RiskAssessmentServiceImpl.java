package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import com.civicsense.complaint.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public RiskAssessment assess(
            Complaint complaint,
            ComplaintAnalysis analysis
    ) {

        RiskAssessment assessment =
                new RiskAssessment();

        assessment.setComplaint(complaint);

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

        // 6. Determine priority
        ComplaintPriority priority =
                determinePriority(riskScore);

        assessment.setPriority(priority);

        // 7. Save only after all required fields are calculated
        RiskAssessment savedAssessment =
                riskAssessmentRepository.save(
                        assessment
                );

        System.out.println("===== RISK ASSESSMENT =====");
        System.out.println("Severity Risk       = " + severityScore);
        System.out.println("Corroboration Risk  = " + corroborationScore);
        System.out.println("Location Risk       = " + locationRiskScore);
        System.out.println("Infrastructure Risk = " + infrastructureRiskScore);
        System.out.println("Final Risk Score    = " + riskScore);
        System.out.println("Priority            = " + priority);
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

        if (complaint.getCluster() == null
                || complaint.getCluster().getComplaintCount() == null) {
            return 0.0;
        }

        int complaintCount =
                complaint.getCluster().getComplaintCount();

        return Math.min(
                complaintCount / 5.0,
                1.0
        ) * 100.0;
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
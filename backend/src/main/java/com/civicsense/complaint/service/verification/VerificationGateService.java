package com.civicsense.complaint.service.verification;

import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.analysis.EvidenceLevel;
import com.civicsense.complaint.entity.analysis.ImageAuthenticity;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic Verification Gate Service.
 *
 * <p>Architectural Rule:
 * AI automates normal complaint processing. Field verification is ONLY for exceptional cases.
 *
 * <p>Initial Rules:
 * <ul>
 *   <li>HIGH/CRITICAL severity or risk + LOW evidence level (E0) -> verificationRequired = true</li>
 *   <li>Conflicting evidence (e.g. descriptionImageMismatch = true) -> verificationRequired = true</li>
 *   <li>Suspicious flags (e.g. imageAuthenticity == SUSPICIOUS or exifLocationMismatch = true) -> verificationRequired = true</li>
 *   <li>Normal complaints (e.g. HIGH + E2 + no suspicious flags, or LOW/MEDIUM severity with E0/E1) -> verificationRequired = false</li>
 * </ul>
 *
 * <p>Crucially: Weak evidence does NOT lower the complaint priority.
 * A CRITICAL + E0 complaint remains CRITICAL priority, but requires field verification.
 */
@Service
public class VerificationGateService {

    public VerificationDecision evaluate(
            ComplaintAnalysis analysis,
            RiskAssessment riskAssessment,
            ComplaintImage complaintImage
    ) {
        List<String> reasons = new ArrayList<>();

        // 1. Check for suspicious flags or evidence conflicts
        if (analysis != null) {
            if (Boolean.TRUE.equals(analysis.getDescriptionImageMismatch())) {
                reasons.add("DESCRIPTION_IMAGE_MISMATCH");
            }
            if (analysis.getImageAuthenticity() == ImageAuthenticity.SUSPICIOUS) {
                reasons.add("SUSPICIOUS_IMAGE_AUTHENTICITY");
            }
        }

        if (complaintImage != null) {
            if (Boolean.TRUE.equals(complaintImage.getExifLocationMismatch())) {
                reasons.add("EXIF_LOCATION_MISMATCH");
            }
            if (Boolean.TRUE.equals(complaintImage.getImageReuseSuspicious())) {
                String typeStr = complaintImage.getImageReuseType() != null
                        ? complaintImage.getImageReuseType().name()
                        : "SUSPICIOUS";
                reasons.add("SUSPICIOUS_IMAGE_REUSE_" + typeStr);
            }
        }

        // 2. Check for high-severity + low evidence level (E0)
        boolean isHighSeverityOrRisk = false;
        if (analysis != null && analysis.getSeverity() != null) {
            if (analysis.getSeverity() == ComplaintSeverity.HIGH || analysis.getSeverity() == ComplaintSeverity.CRITICAL) {
                isHighSeverityOrRisk = true;
            }
        }

        if (riskAssessment != null && riskAssessment.getPriority() != null) {
            switch (riskAssessment.getPriority()) {
                case HIGH, CRITICAL -> isHighSeverityOrRisk = true;
                default -> {}
            }
        }

        EvidenceLevel evidenceLevel = analysis != null ? analysis.getEvidenceLevel() : null;
        if (evidenceLevel == null) {
            evidenceLevel = EvidenceLevel.E0;
        }

        if (isHighSeverityOrRisk && evidenceLevel == EvidenceLevel.E0) {
            reasons.add("HIGH_SEVERITY_LOW_EVIDENCE");
        }

        if (!reasons.isEmpty()) {
            return VerificationDecision.required(String.join(", ", reasons));
        }

        return VerificationDecision.notRequired("NORMAL_FLOW");
    }
}
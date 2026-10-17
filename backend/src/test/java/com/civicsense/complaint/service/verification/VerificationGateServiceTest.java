package com.civicsense.complaint.service.verification;

import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.analysis.EvidenceLevel;
import com.civicsense.complaint.entity.analysis.ImageAuthenticity;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerificationGateServiceTest {

    private VerificationGateService verificationGateService;

    @BeforeEach
    void setUp() {
        verificationGateService = new VerificationGateService();
    }

    @Test
    @DisplayName("No-photo low-risk: LOW severity + E0 -> verificationRequired = false (NORMAL_FLOW)")
    void testNoPhotoLowRisk_DoesNotRequireVerification() {
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.LOW);
        analysis.setEvidenceLevel(EvidenceLevel.E0);
        analysis.setLowEvidence(true);
        analysis.setImageAuthenticity(ImageAuthenticity.NOT_APPLICABLE);
        analysis.setDescriptionImageMismatch(false);

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setPriority(ComplaintPriority.LOW);

        VerificationDecision decision = verificationGateService.evaluate(analysis, riskAssessment, null);

        assertFalse(decision.verificationRequired());
        assertEquals("NORMAL_FLOW", decision.reason());
    }

    @Test
    @DisplayName("No-photo high/critical: CRITICAL severity + E0 -> verificationRequired = true, priority remains CRITICAL")
    void testNoPhotoHighCritical_RequiresVerification() {
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.CRITICAL);
        analysis.setEvidenceLevel(EvidenceLevel.E0);
        analysis.setLowEvidence(true);
        analysis.setImageAuthenticity(ImageAuthenticity.NOT_APPLICABLE);
        analysis.setDescriptionImageMismatch(false);

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setPriority(ComplaintPriority.CRITICAL);

        VerificationDecision decision = verificationGateService.evaluate(analysis, riskAssessment, null);

        assertTrue(decision.verificationRequired());
        assertTrue(decision.reason().contains("HIGH_SEVERITY_LOW_EVIDENCE"));
        // Priority is maintained independently in RiskAssessment
        assertEquals(ComplaintPriority.CRITICAL, riskAssessment.getPriority());
    }

    @Test
    @DisplayName("Photo strong evidence: HIGH severity + E2 + consistent -> verificationRequired = false")
    void testPhotoStrongEvidence_DoesNotRequireVerification() {
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.HIGH);
        analysis.setEvidenceLevel(EvidenceLevel.E2);
        analysis.setLowEvidence(false);
        analysis.setImageAuthenticity(ImageAuthenticity.LIKELY_REAL);
        analysis.setDescriptionImageMismatch(false);

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setPriority(ComplaintPriority.HIGH);

        ComplaintImage image = new ComplaintImage();
        image.setImageUrl("https://example.com/photo.jpg");
        image.setExifLocationMismatch(false);

        VerificationDecision decision = verificationGateService.evaluate(analysis, riskAssessment, image);

        assertFalse(decision.verificationRequired());
        assertEquals("NORMAL_FLOW", decision.reason());
    }

    @Test
    @DisplayName("Suspicious image/location case: SUSPICIOUS image authenticity + EXIF mismatch -> verificationRequired = true")
    void testSuspiciousImageAndLocation_RequiresVerification() {
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.LOW); // Even if low severity
        analysis.setEvidenceLevel(EvidenceLevel.E1);
        analysis.setImageAuthenticity(ImageAuthenticity.SUSPICIOUS);
        analysis.setDescriptionImageMismatch(false);

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setPriority(ComplaintPriority.LOW);

        ComplaintImage image = new ComplaintImage();
        image.setImageUrl("https://example.com/photo.jpg");
        image.setExifLocationMismatch(true);

        VerificationDecision decision = verificationGateService.evaluate(analysis, riskAssessment, image);

        assertTrue(decision.verificationRequired());
        assertTrue(decision.reason().contains("SUSPICIOUS_IMAGE_AUTHENTICITY"));
        assertTrue(decision.reason().contains("EXIF_LOCATION_MISMATCH"));
    }

    @Test
    @DisplayName("Description image mismatch flag -> verificationRequired = true")
    void testDescriptionImageMismatch_RequiresVerification() {
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.MEDIUM);
        analysis.setEvidenceLevel(EvidenceLevel.E0);
        analysis.setImageAuthenticity(ImageAuthenticity.UNCERTAIN);
        analysis.setDescriptionImageMismatch(true);

        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setPriority(ComplaintPriority.MEDIUM);

        ComplaintImage image = new ComplaintImage();
        image.setImageUrl("https://example.com/photo.jpg");
        image.setExifLocationMismatch(false);

        VerificationDecision decision = verificationGateService.evaluate(analysis, riskAssessment, image);

        assertTrue(decision.verificationRequired());
        assertTrue(decision.reason().contains("DESCRIPTION_IMAGE_MISMATCH"));
    }
}
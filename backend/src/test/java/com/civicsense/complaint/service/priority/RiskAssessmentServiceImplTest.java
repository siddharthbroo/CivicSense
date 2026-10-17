package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.RiskAssessmentRepository;
import com.civicsense.complaint.service.community.CitizenCorroborationService;
import com.civicsense.complaint.service.verification.VerificationDecision;
import com.civicsense.complaint.service.verification.VerificationGateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceImplTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private LocationRiskService locationRiskService;
    @Mock
    private InfrastructureRiskService infrastructureRiskService;
    @Mock
    private VerificationGateService verificationGateService;
    @Mock
    private ComplaintImageRepository complaintImageRepository;
    @Mock
    private CitizenCorroborationService citizenCorroborationService;

    @InjectMocks
    private RiskAssessmentServiceImpl riskAssessmentService;

    @BeforeEach
    void setUp() {
        CorroborationIntelligence mockIntel = mock(CorroborationIntelligence.class);
        when(mockIntel.corroborationScore()).thenReturn(10.0);
        when(citizenCorroborationService.analyzeComplaintCorroboration(any())).thenReturn(mockIntel);
        when(locationRiskService.calculateLocationRisk(any())).thenReturn(10.0);
        when(infrastructureRiskService.calculateInfrastructureRisk(any())).thenReturn(10.0);
        when(riskAssessmentRepository.save(any(RiskAssessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationGateService.evaluate(any(), any(), any()))
                .thenReturn(new VerificationDecision(false, "NORMAL_FLOW"));
    }

    @Test
    @DisplayName("calculateSeverityScore: LOW severity produces score 20.0 without NoClassDefFoundError")
    void testAssess_LowSeverity() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.LOW);

        when(riskAssessmentRepository.findByComplaintId(complaint.getId())).thenReturn(Optional.empty());

        RiskAssessment result = riskAssessmentService.assess(complaint, analysis);

        assertNotNull(result);
        assertEquals(20.0, result.getSeverityScore());
    }

    @Test
    @DisplayName("calculateSeverityScore: CRITICAL severity produces score 100.0 without NoClassDefFoundError")
    void testAssess_CriticalSeverity() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(ComplaintSeverity.CRITICAL);

        when(riskAssessmentRepository.findByComplaintId(complaint.getId())).thenReturn(Optional.empty());

        RiskAssessment result = riskAssessmentService.assess(complaint, analysis);

        assertNotNull(result);
        assertEquals(100.0, result.getSeverityScore());
    }

    @Test
    @DisplayName("calculateSeverityScore: MEDIUM and HIGH severities produce scores 50.0 and 75.0")
    void testAssess_MediumAndHighSeverity() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());

        ComplaintAnalysis analysisMed = new ComplaintAnalysis();
        analysisMed.setSeverity(ComplaintSeverity.MEDIUM);

        when(riskAssessmentRepository.findByComplaintId(complaint.getId())).thenReturn(Optional.empty());

        RiskAssessment resultMed = riskAssessmentService.assess(complaint, analysisMed);
        assertEquals(50.0, resultMed.getSeverityScore());

        ComplaintAnalysis analysisHigh = new ComplaintAnalysis();
        analysisHigh.setSeverity(ComplaintSeverity.HIGH);

        RiskAssessment resultHigh = riskAssessmentService.assess(complaint, analysisHigh);
        assertEquals(75.0, resultHigh.getSeverityScore());
    }

    @Test
    @DisplayName("calculateSeverityScore: null severity produces score 0.0")
    void testAssess_NullSeverity() {
        Complaint complaint = new Complaint();
        complaint.setId(UUID.randomUUID());

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setSeverity(null);

        when(riskAssessmentRepository.findByComplaintId(complaint.getId())).thenReturn(Optional.empty());

        RiskAssessment result = riskAssessmentService.assess(complaint, analysis);

        assertNotNull(result);
        assertEquals(0.0, result.getSeverityScore());
    }
}


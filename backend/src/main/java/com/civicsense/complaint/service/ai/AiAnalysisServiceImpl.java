package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.*;
import com.civicsense.complaint.entity.analysis.AiAnalysisStatus;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.EvidenceLevel;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

        private final ComplaintAnalysisRepository complaintAnalysisRepository;
        private final GeminiAiService geminiAiService;
        private final ObjectMapper objectMapper;
        private final EvidenceLevelService evidenceLevelService;

        @Override
        @Transactional
        public AiAnalysisResult analyze(
                        Complaint complaint,
                        ComplaintImage complaintImage) {

                // Create analysis record first
                ComplaintAnalysis analysis = new ComplaintAnalysis();

                analysis.setComplaint(complaint);
                analysis.setStatus(AiAnalysisStatus.PENDING);

                try {

                        // 1. Send complaint + image to Gemini (M3)
                        String aiResponse = geminiAiService.analyzeComplaint(
                                        complaint,
                                        complaintImage);

                        System.out.println("===== GEMINI RESPONSE =====");
                        System.out.println(aiResponse);
                        System.out.println("===========================");

                        // 2. Convert Gemini JSON into Java object
                        AiAnalysisResult result = objectMapper.readValue(
                                        aiResponse,
                                        AiAnalysisResult.class);

                        // 3. Fill analysis entity from Gemini result
                        analysis.setLanguage(result.language());
                        analysis.setCategory(result.category());
                        analysis.setSeverity(result.severity());
                        analysis.setSummary(result.summary());
                        analysis.setDepartment(result.department());

                        analysis.setConfidence(
                                        result.confidence());

                        analysis.setImageAuthenticity(
                                        result.imageAuthenticity());

                        analysis.setDescriptionImageConsistency(
                                        result.descriptionImageConsistency());

                        analysis.setSafetyRisk(
                                        result.safetyRisk());

                        analysis.setImmediateHazard(
                                        result.immediateHazard() != null && result.immediateHazard());

                        analysis.setPublicImpact(
                                        result.publicImpact());

                        analysis.setPotentialHarm(
                                        result.potentialHarm());

                        analysis.setRecommendedUrgency(
                                        result.recommendedUrgency());

                        // 4. Compute Evidence Level deterministically (E0–E3)
                        //    This runs AFTER Gemini so we have the consistency signal.
                        DescriptionImageConsistency consistency =
                                        result.descriptionImageConsistency();

                        EvidenceLevel evidenceLevel =
                                        evidenceLevelService.compute(complaintImage, consistency);

                        analysis.setEvidenceLevel(evidenceLevel);

                        // 5. Set deterministic flags (informational; do not block complaint)

                        // LOW_EVIDENCE: set when no photo was submitted (E0)
                        boolean isLowEvidence = evidenceLevel == EvidenceLevel.E0;
                        analysis.setLowEvidence(isLowEvidence);

                        // DESCRIPTION_IMAGE_MISMATCH: set when image is present and Gemini
                        // reported INCONSISTENT (never raised for text-only complaints)
                        boolean descImageMismatch =
                                        complaintImage != null
                                                        && consistency == DescriptionImageConsistency.INCONSISTENT;
                        analysis.setDescriptionImageMismatch(descImageMismatch);

                        System.out.println("===== EVIDENCE ASSESSMENT =====");
                        System.out.println("Evidence Level         = " + evidenceLevel);
                        System.out.println("Low Evidence Flag      = " + isLowEvidence);
                        System.out.println("Desc-Image Mismatch    = " + descImageMismatch);
                        System.out.println("===============================");

                        // 6. Mark analysis as completed
                        analysis.setStatus(
                                        AiAnalysisStatus.COMPLETED);

                        analysis.setErrorMessage(null);

                        // 7. Save successful analysis
                        complaintAnalysisRepository.save(analysis);

                        System.out.println(
                                        "AI ANALYSIS SAVED = "
                                                        + analysis.getId());

                        return result;

                } catch (Exception e) {

                        // AI analysis failed — do NOT crash the complaint submission
                        analysis.setStatus(
                                        AiAnalysisStatus.FAILED);

                        analysis.setErrorMessage(
                                        e.getMessage());

                        // Save failure information
                        complaintAnalysisRepository.save(analysis);

                        System.err.println(
                                        "AI ANALYSIS FAILED = "
                                                        + e.getMessage());

                        return null;
                }
        }
}
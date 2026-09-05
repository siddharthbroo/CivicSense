package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.*;
import com.civicsense.complaint.entity.analysis.AiAnalysisStatus;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final ComplaintAnalysisRepository complaintAnalysisRepository;
    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiAnalysisResult analyze(
            Complaint complaint,
            ComplaintImage complaintImage
    ) {

        // Create analysis record first
        ComplaintAnalysis analysis =
                new ComplaintAnalysis();

        analysis.setComplaint(complaint);
        analysis.setStatus(AiAnalysisStatus.PENDING);

        try {

            // 1. Send complaint + image to Gemini
            String aiResponse =
                    geminiAiService.analyzeComplaint(
                            complaint,
                            complaintImage
                    );

            System.out.println("===== GEMINI RESPONSE =====");
            System.out.println(aiResponse);
            System.out.println("===========================");

            // 2. Convert Gemini JSON into Java object
            AiAnalysisResult result =
                    objectMapper.readValue(
                            aiResponse,
                            AiAnalysisResult.class
                    );

            // 3. Fill analysis entity
            analysis.setLanguage(result.language());
            analysis.setCategory(result.category());
            analysis.setSeverity(result.severity());
            analysis.setSummary(result.summary());
            analysis.setDepartment(result.department());

            analysis.setConfidence(
                    result.confidence()
            );

            analysis.setImageAuthenticity(
                    result.imageAuthenticity()
            );

            analysis.setDescriptionImageConsistency(
                    result.descriptionImageConsistency()
            );

            // 4. Mark analysis as completed
            analysis.setStatus(
                    AiAnalysisStatus.COMPLETED
            );

            analysis.setErrorMessage(null);

            // 5. Save successful analysis
            complaintAnalysisRepository.save(analysis);

            System.out.println(
                    "AI ANALYSIS SAVED = "
                            + analysis.getId()
            );

            return result;

        } catch (Exception e) {

            // AI analysis failed
            analysis.setStatus(
                    AiAnalysisStatus.FAILED
            );

            analysis.setErrorMessage(
                    e.getMessage()
            );

            // Save failure information
            complaintAnalysisRepository.save(analysis);

            System.err.println(
                    "AI ANALYSIS FAILED = "
                            + e.getMessage()
            );

            // Don't let AI failure crash complaint submission
            return null;
        }
    }
}
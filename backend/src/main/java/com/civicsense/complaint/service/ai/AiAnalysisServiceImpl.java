package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintAnalysis;
import com.civicsense.complaint.entity.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final ComplaintAnalysisRepository complaintAnalysisRepository;

    @Override
    public AiAnalysisResult analyze(
            Complaint complaint,
            ComplaintImage complaintImage
    ) {

        /*
         * Temporary AI result.
         *
         * Actual AI integration will be added later.
         */
        AiAnalysisResult result = new AiAnalysisResult(
                "UNKNOWN",
                "UNCLASSIFIED",
                "UNKNOWN",
                "AI analysis pending",
                "UNKNOWN",
                0.0,
                "UNKNOWN",
                "UNKNOWN"
        );

        // Create analysis record
        ComplaintAnalysis analysis =
                new ComplaintAnalysis();

        analysis.setComplaint(complaint);
        analysis.setLanguage(result.language());
        analysis.setCategory(result.category());
        analysis.setSeverity(result.severity());
        analysis.setSummary(result.summary());
        analysis.setDepartment(result.department());
        analysis.setConfidence(result.confidence());
        analysis.setImageAuthenticity(
                result.imageAuthenticity()
        );
        analysis.setDescriptionImageConsistency(
                result.descriptionImageConsistency()
        );

        // Save AI analysis
        complaintAnalysisRepository.save(analysis);

        return result;
    }
}
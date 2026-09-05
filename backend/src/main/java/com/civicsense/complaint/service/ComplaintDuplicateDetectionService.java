package com.civicsense.complaint.service;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.service.ai.ComplaintTextSimilarityResponse;
import com.civicsense.complaint.service.ai.PythonAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintDuplicateDetectionService {

    private final ComplaintAnalysisRepository complaintAnalysisRepository;
    private final PythonAiClient pythonAiClient;
    private final ComplaintDuplicateScoringService scoringService;
    private final ComplaintImageRepository complaintImageRepository;

    public DuplicateDetectionResult detectDuplicates(
            Complaint complaint,
            ComplaintAnalysis analysis
    ) {

        List<ComplaintAnalysis> candidates =
                complaintAnalysisRepository
                        .findByCategoryAndComplaintIdNot(
                                analysis.getCategory(),
                                complaint.getId()
                        );

        if (candidates.isEmpty()) {
            return new DuplicateDetectionResult(
                    ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                    null,
                    null
            );
        }

        return candidates.stream()
                .map(candidate -> compare(
                        complaint,
                        analysis,
                        candidate
                ))
                .max(Comparator.comparingDouble(
                        DuplicateCandidateResult::score
                ))
                .map(result -> new DuplicateDetectionResult(
                        result.decision(),
                        result.complaint(),
                        result.score()
                ))
                .orElse(
                        new DuplicateDetectionResult(
                                ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                                null,
                                null
                        )
                );
    }

    private DuplicateCandidateResult compare(
            Complaint complaint,
            ComplaintAnalysis analysis,
            ComplaintAnalysis candidateAnalysis
    ) {

        Complaint candidateComplaint =
                candidateAnalysis.getComplaint();

        /*
         * ==========================================
         * 1. IMAGE SIMILARITY
         * ==========================================
         */

        ComplaintImage newComplaintImage =
                complaintImageRepository
                        .findTopByComplaintIdOrderByCreatedAtDesc(
                                complaint.getId()
                        )
                        .orElse(null);

        ComplaintImage candidateComplaintImage =
                complaintImageRepository
                        .findTopByComplaintIdOrderByCreatedAtDesc(
                                candidateComplaint.getId()
                        )
                        .orElse(null);

        double imageSimilarity = 0.0;

        if (newComplaintImage != null
                && candidateComplaintImage != null) {

            imageSimilarity =
                    pythonAiClient.calculateImageSimilarity(
                            newComplaintImage.getImageUrl(),
                            candidateComplaintImage.getImageUrl()
                    );
        }

        /*
         * ==========================================
         * 2. ADDRESS SIMILARITY
         * ==========================================
         */

        double addressSimilarity = 0.0;

        if (complaint.getAddress() != null
                && candidateComplaint.getAddress() != null
                && !complaint.getAddress().isBlank()
                && !candidateComplaint.getAddress().isBlank()) {

            addressSimilarity =
                    pythonAiClient.calculateAddressSimilarity(
                            complaint.getAddress(),
                            candidateComplaint.getAddress()
                    );
        }

        /*
         * ==========================================
         * 3. LOCATION DISTANCE
         * ==========================================
         */

        double distanceMeters =
                pythonAiClient.calculateLocationDistance(
                        complaint.getLatitude(),
                        complaint.getLongitude(),
                        candidateComplaint.getLatitude(),
                        candidateComplaint.getLongitude()
                );

        /*
         * ==========================================
         * 2. TEXT SIMILARITY
         * ==========================================
         */

        ComplaintTextSimilarityResponse textSimilarity =
                pythonAiClient.compareComplaints(
                        complaint.getTitle(),
                        complaint.getDescription(),
                        analysis.getSummary(),
                        analysis.getCategory().name(),

                        candidateComplaint.getTitle(),
                        candidateComplaint.getDescription(),
                        candidateAnalysis.getSummary(),
                        candidateAnalysis.getCategory().name()
                );

        /*
         * ==========================================
         * 3. COMBINED SCORE
         * ==========================================
         *
         * Address and location will be added
         * in the next steps.
         */

        System.out.println("===== SIMILARITY DETAILS =====");
        System.out.println("Title Similarity       = " + textSimilarity.title_similarity());
        System.out.println("Description Similarity = " + textSimilarity.description_similarity());
        System.out.println("Summary Similarity     = " + textSimilarity.summary_similarity());
        System.out.println("Image Similarity       = " + imageSimilarity);
        System.out.println("Address Similarity     = " + addressSimilarity);
        System.out.println("Location Distance      = " + distanceMeters);
        System.out.println("==============================");

        double score =
                scoringService.calculateCombinedScore(
                        textSimilarity.title_similarity(),
                        textSimilarity.description_similarity(),
                        textSimilarity.summary_similarity(),
                        imageSimilarity,
                        addressSimilarity,
                        distanceMeters
                );

        /*
         * ==========================================
         * 4. DUPLICATE DECISION
         * ==========================================
         */

        ComplaintDuplicateScoringService.DuplicateDecision decision =
                scoringService.decide(
                        score,
                        textSimilarity.category_match()
                );

        return new DuplicateCandidateResult(
                candidateComplaint,
                score,
                decision
        );
    }

    private record DuplicateCandidateResult(
            Complaint complaint,
            double score,
            ComplaintDuplicateScoringService.DuplicateDecision decision
    ) {
    }

    public record DuplicateDetectionResult(
            ComplaintDuplicateScoringService.DuplicateDecision decision,
            Complaint matchedComplaint,
            Double score
    ) {
    }
}
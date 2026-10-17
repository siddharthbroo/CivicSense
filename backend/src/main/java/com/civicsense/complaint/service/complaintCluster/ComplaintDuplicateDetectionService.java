package com.civicsense.complaint.service.complaintCluster;

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

        if (complaint.getLatitude() == null || complaint.getLongitude() == null || analysis.getCategory() == null) {
            return new DuplicateDetectionResult(
                    ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        double latDelta = 0.005; // ~550m bounding box (well covers the 50m max threshold)
        double lonDelta = 0.005;

        List<ComplaintAnalysis> candidates;
        try {
            candidates = complaintAnalysisRepository.findCandidates(
                    analysis.getCategory(),
                    complaint.getId(),
                    complaint.getLatitude() - latDelta,
                    complaint.getLatitude() + latDelta,
                    complaint.getLongitude() - lonDelta,
                    complaint.getLongitude() + lonDelta
            );
        } catch (Exception e) {
            System.out.println("Candidate search failed: " + e.getMessage());
            return new DuplicateDetectionResult(
                    ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (candidates.isEmpty()) {
            return new DuplicateDetectionResult(
                    ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                    null,
                    null,
                    null,
                    null,
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
                        result.score(),
                        result.textSimilarity(),
                        result.imageSimilarity(),
                        result.addressSimilarity(),
                        result.distanceMeters()
                ))
                .orElse(
                        new DuplicateDetectionResult(
                                ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                                null,
                                null,
                                null,
                                null,
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
            try {
                imageSimilarity =
                        pythonAiClient.calculateImageSimilarity(
                                newComplaintImage.getImageUrl(),
                                candidateComplaintImage.getImageUrl()
                        );
            } catch (Exception e) {
                System.out.println("Image similarity calculation failed: " + e.getMessage());
            }
        }

        /*
         * ==========================================
         * 2. ADDRESS SIMILARITY
         * ==========================================
         */

        boolean hasAddressComparison = complaint.getAddress() != null
                && candidateComplaint.getAddress() != null
                && !complaint.getAddress().isBlank()
                && !candidateComplaint.getAddress().isBlank();

        double addressSimilarity = 0.0;

        if (hasAddressComparison) {
            try {
                addressSimilarity =
                        pythonAiClient.calculateAddressSimilarity(
                                complaint.getAddress(),
                                candidateComplaint.getAddress()
                        );
            } catch (Exception e) {
                System.out.println("Address similarity calculation failed: " + e.getMessage());
                hasAddressComparison = false;
            }
        }

        /*
         * ==========================================
         * 3. LOCATION DISTANCE
         * ==========================================
         */

        double distanceMeters = 99999.0;
        try {
            distanceMeters =
                    pythonAiClient.calculateLocationDistance(
                            complaint.getLatitude(),
                            complaint.getLongitude(),
                            candidateComplaint.getLatitude(),
                            candidateComplaint.getLongitude()
                    );
        } catch (Exception e) {
            System.out.println("Location distance calculation failed: " + e.getMessage());
        }

        /*
         * ==========================================
         * 4. TEXT SIMILARITY
         * ==========================================
         */

        ComplaintTextSimilarityResponse textSimilarity;
        try {
            textSimilarity =
                    pythonAiClient.compareComplaints(
                            complaint.getTitle(),
                            complaint.getDescription(),
                            analysis.getSummary(),
                            analysis.getCategory() != null ? analysis.getCategory().name() : "",

                            candidateComplaint.getTitle(),
                            candidateComplaint.getDescription(),
                            candidateAnalysis.getSummary(),
                            candidateAnalysis.getCategory() != null ? candidateAnalysis.getCategory().name() : ""
                    );
        } catch (Exception e) {
            System.out.println("Text similarity comparison failed: " + e.getMessage());
            textSimilarity = new ComplaintTextSimilarityResponse(0.0, 0.0, 0.0, true);
        }

        /*
         * ==========================================
         * 5. SIMILARITY DETAILS
         * ==========================================
         */

        System.out.println(
                "===== SIMILARITY DETAILS ====="
        );

        System.out.println(
                "Title Similarity       = "
                        + textSimilarity.title_similarity()
        );

        System.out.println(
                "Description Similarity = "
                        + textSimilarity.description_similarity()
        );

        System.out.println(
                "Summary Similarity     = "
                        + textSimilarity.summary_similarity()
        );

        System.out.println(
                "Image Similarity       = "
                        + imageSimilarity
        );

        System.out.println(
                "Address Similarity     = "
                        + addressSimilarity
        );

        System.out.println(
                "Location Distance      = "
                        + distanceMeters
        );

        System.out.println(
                "=============================="
        );

        /*
         * ==========================================
         * 6. COMBINED SCORE
         * ==========================================
         */

        // hasImageComparison is true only when BOTH complaints have an image.
        boolean hasImageComparison =
                newComplaintImage != null && candidateComplaintImage != null;

        double score =
                scoringService.calculateCombinedScore(
                        textSimilarity.title_similarity(),
                        textSimilarity.description_similarity(),
                        textSimilarity.summary_similarity(),
                        imageSimilarity,
                        addressSimilarity,
                        distanceMeters,
                        hasAddressComparison,
                        hasImageComparison
                );

        /*
         * ==========================================
         * 7. DUPLICATE DECISION
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
                decision,
                textSimilarity,
                imageSimilarity,
                addressSimilarity,
                distanceMeters
        );
    }

    /*
     * ==========================================
     * INTERNAL RESULT FOR EACH CANDIDATE
     * ==========================================
     */

    private record DuplicateCandidateResult(
            Complaint complaint,
            double score,
            ComplaintDuplicateScoringService.DuplicateDecision decision,
            ComplaintTextSimilarityResponse textSimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters
    ) {
    }

    /*
     * ==========================================
     * FINAL DUPLICATE DETECTION RESULT
     * ==========================================
     */

    public record DuplicateDetectionResult(
            ComplaintDuplicateScoringService.DuplicateDecision decision,
            Complaint matchedComplaint,
            Double score,
            ComplaintTextSimilarityResponse textSimilarity,
            Double imageSimilarity,
            Double addressSimilarity,
            Double distanceMeters
    ) {
    }
}
package com.civicsense.complaint.service;

import com.civicsense.complaint.dto.ComplaintResponse;
import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.service.ai.AiAnalysisResult;
import com.civicsense.complaint.service.ai.AiAnalysisService;
import com.civicsense.complaint.service.ai.AiDuplicateVerificationResult;
import com.civicsense.complaint.service.ai.ComplaintTextSimilarityResponse;
import com.civicsense.complaint.service.ai.GeminiAiService;
import com.civicsense.complaint.service.complaintCluster.ComplaintClusterService;
import com.civicsense.complaint.service.complaintCluster.ComplaintDuplicateDetectionService;
import com.civicsense.complaint.service.complaintCluster.ComplaintDuplicateScoringService;
import com.civicsense.complaint.service.image.ImageKitService;
import com.civicsense.complaint.service.image.ImageUploadResult;
import com.civicsense.complaint.service.priority.RiskAssessmentService;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

        private final ComplaintRepository complaintRepository;
        private final ComplaintImageRepository complaintImageRepository;
        private final UserRepository userRepository;
        private final ImageKitService imageKitService;
        private final AiAnalysisService aiAnalysisService;
        private final ComplaintAnalysisRepository complaintAnalysisRepository;
        private final ComplaintDuplicateDetectionService duplicateDetectionService;
        private final ComplaintClusterService complaintClusterService;
        private final GeminiAiService geminiAiService;
        private final ObjectMapper objectMapper;
        private final RiskAssessmentService riskAssessmentService;

        @Override
        @Transactional
        public CreateComplaintResponse createComplaint(
                        UUID userId,
                        CreateComplaintRequest request,
                        MultipartFile image) {

                // 1. Image is mandatory
                if (image == null || image.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Complaint image is required");
                }

                // 2. Find logged-in user
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 3. Create complaint
                Complaint complaint = new Complaint();

                complaint.setUser(user);
                complaint.setDescription(request.description());
                complaint.setLatitude(request.latitude());
                complaint.setLongitude(request.longitude());
                complaint.setAddress(request.address());
                complaint.setStatus(ComplaintStatus.SUBMITTED);

                // 4. Save complaint
                Complaint savedComplaint = complaintRepository.save(complaint);

                // 5. Upload image to ImageKit
                ImageUploadResult uploadResult = imageKitService.uploadImage(image);

                // 6. Create image record
                ComplaintImage complaintImage = new ComplaintImage();

                complaintImage.setComplaint(savedComplaint);
                complaintImage.setImageUrl(
                                uploadResult.imageUrl());
                complaintImage.setStorageKey(
                                uploadResult.storageKey());
                complaintImage.setContentType(
                                image.getContentType());

                // 7. Save image reference
                complaintImageRepository.save(complaintImage);

                // 8. Analyze complaint using Gemini
                AiAnalysisResult aiAnalysisResult = aiAnalysisService.analyze(
                                savedComplaint,
                                complaintImage);

                if (aiAnalysisResult != null) {

                        // 9. Save Gemini generated title and category
                        savedComplaint.setTitle(
                                        aiAnalysisResult.title());
                        if (aiAnalysisResult.category() != null) {
                                savedComplaint.setCategory(
                                                aiAnalysisResult.category().name());
                        }

                        complaintRepository.save(savedComplaint);

                        // 10. Get saved AI analysis
                        ComplaintAnalysis analysis = complaintAnalysisRepository
                                        .findByComplaintId(
                                                        savedComplaint.getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Complaint analysis not found"));

                        // 11. Detect duplicate complaints
                        ComplaintDuplicateDetectionService.DuplicateDetectionResult duplicateResult;
                        try {
                                duplicateResult = duplicateDetectionService.detectDuplicates(
                                                savedComplaint,
                                                analysis);
                        } catch (Exception e) {
                                System.out.println("Duplicate detection failed: " + e.getMessage());
                                duplicateResult = new ComplaintDuplicateDetectionService.DuplicateDetectionResult(
                                                ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
                        }

                        System.out.println(
                                        "===== DUPLICATE DETECTION =====");

                        System.out.println(
                                        "Decision = "
                                                        + duplicateResult.decision());

                        System.out.println(
                                        "Score = "
                                                        + duplicateResult.score());

                        /*
                         * ==========================================
                         * CANDIDATE DUPLICATE (DUPLICATE / POTENTIAL_DUPLICATE)
                         * All candidates undergo Stage-2 Gemini visual verification
                         * ==========================================
                         */

                        if ((duplicateResult.decision() == ComplaintDuplicateScoringService.DuplicateDecision.DUPLICATE
                                        || duplicateResult
                                                        .decision() == ComplaintDuplicateScoringService.DuplicateDecision.POTENTIAL_DUPLICATE)
                                        && duplicateResult.matchedComplaint() != null) {

                                System.out.println(
                                                "Candidate duplicate detected (Decision: "
                                                                + duplicateResult.decision()
                                                                + "). Starting Gemini verification...");

                                // Get matched complaint's AI analysis
                                ComplaintAnalysis matchedAnalysis = complaintAnalysisRepository
                                                .findByComplaintId(
                                                                duplicateResult
                                                                                .matchedComplaint()
                                                                                .getId())
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Matched complaint analysis not found"));

                                // Get matched complaint's latest image
                                ComplaintImage matchedImage = complaintImageRepository
                                                .findTopByComplaintIdOrderByCreatedAtDesc(
                                                                duplicateResult
                                                                                .matchedComplaint()
                                                                                .getId())
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Image for matched complaint not found"));

                                // Get Stage-1 text similarity evidence
                                ComplaintTextSimilarityResponse textSimilarity = duplicateResult.textSimilarity();

                                // Send complete evidence to Gemini Stage-2
                                String verificationResult = geminiAiService.verifyDuplicateComplaints(
                                                savedComplaint,
                                                complaintImage,
                                                analysis,

                                                duplicateResult.matchedComplaint(),
                                                matchedImage,
                                                matchedAnalysis,

                                                textSimilarity != null ? textSimilarity.title_similarity() : 0.0,
                                                textSimilarity != null ? textSimilarity.description_similarity() : 0.0,
                                                textSimilarity != null ? textSimilarity.summary_similarity() : 0.0,

                                                duplicateResult.imageSimilarity() != null
                                                                ? duplicateResult.imageSimilarity()
                                                                : 0.0,
                                                duplicateResult.addressSimilarity() != null
                                                                ? duplicateResult.addressSimilarity()
                                                                : 0.0,
                                                duplicateResult.distanceMeters() != null
                                                                ? duplicateResult.distanceMeters()
                                                                : 99999.0);

                                System.out.println(
                                                "===== GEMINI DUPLICATE VERIFICATION =====");

                                System.out.println(
                                                verificationResult);

                                System.out.println(
                                                "==========================================");

                                /*
                                 * ==========================================
                                 * PARSE GEMINI RESULT
                                 * ==========================================
                                 */

                                try {

                                        AiDuplicateVerificationResult verification = objectMapper.readValue(
                                                        verificationResult,
                                                        AiDuplicateVerificationResult.class);

                                        System.out.println(
                                                        "Gemini Verdict = "
                                                                        + verification.verdict());

                                        System.out.println(
                                                        "Gemini Confidence = "
                                                                        + verification.confidence());

                                        System.out.println(
                                                        "Gemini Reasoning = "
                                                                        + verification.reasoning());

                                        /*
                                         * ==========================================
                                         * GEMINI: SAME
                                         * ==========================================
                                         */

                                        if ("SAME".equalsIgnoreCase(
                                                        verification.verdict())) {

                                                complaintClusterService
                                                                .assignToExistingCluster(
                                                                                savedComplaint,
                                                                                duplicateResult
                                                                                                .matchedComplaint());

                                                System.out.println(
                                                                "Gemini verified SAME. "
                                                                                + "Complaint assigned to existing cluster.");
                                        }

                                        /*
                                         * ==========================================
                                         * GEMINI: DIFFERENT
                                         * ==========================================
                                         */

                                        else if ("DIFFERENT".equalsIgnoreCase(
                                                        verification.verdict())) {

                                                complaintClusterService
                                                                .createNewCluster(
                                                                                savedComplaint,
                                                                                analysis.getCategory());

                                                System.out.println(
                                                                "Gemini verified DIFFERENT. "
                                                                                + "New complaint cluster created.");
                                        }

                                        /*
                                         * ==========================================
                                         * GEMINI: UNCERTAIN
                                         * ==========================================
                                         */

                                        else {

                                                complaintClusterService
                                                                .createNewCluster(
                                                                                savedComplaint,
                                                                                analysis.getCategory());

                                                System.out.println(
                                                                "Gemini returned UNCERTAIN. "
                                                                                + "New complaint cluster created.");
                                        }

                                } catch (Exception e) {

                                        System.out.println(
                                                        "Gemini duplicate verification failed: "
                                                                        + e.getMessage());

                                        /*
                                         * Safety fallback:
                                         * Do NOT merge with an existing cluster
                                         * when verification fails.
                                         */

                                        complaintClusterService.createNewCluster(
                                                        savedComplaint,
                                                        analysis.getCategory());

                                        System.out.println(
                                                        "Verification failed. "
                                                                        + "New complaint cluster created as fallback.");
                                }
                        }

                        /*
                         * ==========================================
                         * SEPARATE
                         * ==========================================
                         */

                        else {

                                complaintClusterService.createNewCluster(
                                                savedComplaint,
                                                analysis.getCategory());

                                System.out.println(
                                                "New complaint cluster created");

                                System.out.println(
                                                "===============================");
                        }

                        /*
                         * ==========================================
                         * M5: RISK ASSESSMENT
                         * ==========================================
                         */

                        riskAssessmentService.assess(
                                        savedComplaint,
                                        analysis);

                        System.out.println(
                                        "Risk assessment completed successfully.");

                } else {
                        // Fallback if initial Gemini analysis failed
                        complaintClusterService.createNewCluster(
                                        savedComplaint,
                                        com.civicsense.complaint.entity.classification.ComplaintCategory.OTHER);

                        System.out.println(
                                        "Initial AI analysis failed. New complaint cluster created as fallback.");
                }

                // 12. Return response
                return new CreateComplaintResponse(
                                savedComplaint.getId(),
                                savedComplaint.getStatus(),
                                savedComplaint.getCreatedAt(),
                                "Complaint submitted successfully");
        }

        @Override
        @Transactional(readOnly = true)
        public List<ComplaintResponse> getMyComplaints(
                        UUID userId) {

                List<Complaint> complaints = complaintRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                                userId);

                return complaints.stream()
                                .map(complaint -> {

                                        String imageUrl = complaintImageRepository
                                                        .findTopByComplaintIdOrderByCreatedAtDesc(
                                                                        complaint.getId())
                                                        .map(ComplaintImage::getImageUrl)
                                                        .orElse(null);

                                        return new ComplaintResponse(
                                                        complaint.getId(),
                                                        complaint.getDescription(),
                                                        complaint.getLatitude(),
                                                        complaint.getLongitude(),
                                                        complaint.getAddress(),
                                                        complaint.getStatus(),
                                                        complaint.getCreatedAt(),
                                                        imageUrl);
                                })
                                .toList();
        }
}
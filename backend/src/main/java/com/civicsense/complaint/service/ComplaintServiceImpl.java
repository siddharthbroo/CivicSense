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
import com.civicsense.complaint.service.geo.GeographicHierarchyService;
import com.civicsense.complaint.service.image.ExifMetadata;
import com.civicsense.complaint.service.image.ExifMetadataExtractorService;
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
        private final ExifMetadataExtractorService exifMetadataExtractorService;
        private final GeographicHierarchyService geographicHierarchyService;
        private final com.civicsense.complaint.service.geo.LocationValidationService locationValidationService;
        private final com.civicsense.complaint.service.image.ImageModerationService imageModerationService;
        private final com.civicsense.complaint.service.image.ImageReuseDetectionService imageReuseDetectionService;

        @Override
        @Transactional
        public CreateComplaintResponse createComplaint(
                        UUID userId,
                        CreateComplaintRequest request,
                        MultipartFile image) {

                // 1. Authoritative backend validation of incident coordinates & supported service area
                locationValidationService.validateOrThrow(
                                request.latitude(),
                                request.longitude(),
                                null
                );

                // 2. Find logged-in user
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 3. Create complaint with citizen-confirmed map pin as PRIMARY incident location
                Complaint complaint = new Complaint();

                complaint.setUser(user);
                complaint.setDescription(request.description());
                complaint.setLatitude(request.latitude());
                complaint.setLongitude(request.longitude());
                complaint.setAddress(request.address());

                // Device telemetry (optional hint/suggestion only; never overrides confirmed pin)
                complaint.setDeviceLatitude(request.deviceLatitude());
                complaint.setDeviceLongitude(request.deviceLongitude());
                complaint.setLocationSource(request.locationSource() != null ? request.locationSource() : "MANUAL_PIN");
                complaint.setLocationAccuracy(request.locationAccuracy());

                // Resolve Delhi NCR Administrative Ward based strictly on the confirmed map pin
                geographicHierarchyService.resolveWard(request.latitude(), request.longitude())
                                .ifPresent(complaint::setWard);

                complaint.setStatus(ComplaintStatus.SUBMITTED);

                // 3. Save complaint
                Complaint savedComplaint = complaintRepository.save(complaint);

                // 4. Handle optional image upload: file validation, content moderation, fail-safe quarantine & EXIF stripping
                ComplaintImage complaintImage = null;
                if (image != null && !image.isEmpty()) {
                        // Validate file type, size, magic bytes, and decodability
                        var fileValidation = imageModerationService.validateFileIntegrity(image);
                        if (!fileValidation.valid()) {
                                throw new IllegalArgumentException("Invalid image: " + fileValidation.reason());
                        }

                        byte[] rawBytes;
                        try {
                                rawBytes = image.getBytes();
                        } catch (Exception e) {
                                throw new IllegalArgumentException("Failed to read image bytes: " + e.getMessage());
                        }

                        // Evaluate content safety AND complaint relevance via Gemini pre-screen
                        var moderationResult = imageModerationService.evaluateImageSafetyAndRelevance(
                                        rawBytes,
                                        image.getContentType(),
                                        null,
                                        request.description());

                        // If high-confidence prohibited: REJECT immediately. Do NOT upload or send to Gemini analysis.
                        if (moderationResult.prohibited()) {
                                throw new IllegalArgumentException("Uploaded image violates safety policies: " + moderationResult.reason());
                        }

                        // If clearly unrelated: REJECT immediately.
                        if (moderationResult.unrelated()) {
                                throw new IllegalArgumentException("Uploaded image is unrelated to the complaint: " + moderationResult.reason());
                        }

                        // Extract EXIF metadata server-side (supporting hint only; not exposed publicly)
                        ExifMetadata exif = exifMetadataExtractorService.extractMetadata(
                                        image,
                                        request.latitude(),
                                        request.longitude());

                        // If quarantined (uncertain / moderation service failure), do NOT publish to cloud storage or send to Gemini
                        if (moderationResult.quarantined()) {
                                System.err.println("Complaint image quarantined: " + moderationResult.reason());
                                // Quarantine mode: Complaint created cleanly, image withheld from normal processing/public storage
                        } else {
                                // Image is SAFE: Safely strip EXIF metadata before public upload
                                byte[] safeBytes = exifMetadataExtractorService.stripExifMetadata(
                                                rawBytes,
                                                image.getContentType());

                                // Upload stripped image to cloud storage
                                ImageUploadResult uploadResult = imageKitService.uploadImage(
                                                safeBytes,
                                                image.getOriginalFilename());

                                complaintImage = new ComplaintImage();
                                complaintImage.setComplaint(savedComplaint);
                                complaintImage.setImageUrl(uploadResult.imageUrl());
                                complaintImage.setStorageKey(uploadResult.storageKey());
                                complaintImage.setContentType(image.getContentType());

                                // Attach EXIF metadata
                                complaintImage.setExifLatitude(exif.latitude());
                                complaintImage.setExifLongitude(exif.longitude());
                                complaintImage.setExifTimestamp(exif.timestamp());
                                complaintImage.setHasExifGps(exif.hasGps());
                                complaintImage.setExifDistanceMeters(exif.distanceMetersToPin());
                                complaintImage.setExifLocationMismatch(exif.locationMismatchFlag());

                                // Detect image reuse & calculate SHA-256 + perceptual dHash
                                var reuseResult = imageReuseDetectionService.detectReuse(
                                                rawBytes,
                                                user.getId(),
                                                request.latitude(),
                                                request.longitude(),
                                                savedComplaint.getId()
                                );

                                complaintImage.setImageHash(reuseResult.imageHash());
                                complaintImage.setPerceptualHash(reuseResult.perceptualHash());
                                complaintImage.setImageReused(reuseResult.reused());
                                complaintImage.setImageReuseType(reuseResult.reuseType());
                                complaintImage.setImageReuseSuspicious(reuseResult.suspicious());

                                complaintImageRepository.save(complaintImage);
                        }
                }

                // 5. Analyze complaint using Gemini (multimodal if image present, text-only if no photo)
                AiAnalysisResult aiAnalysisResult = aiAnalysisService.analyze(
                                savedComplaint,
                                complaintImage);

                if (aiAnalysisResult != null) {

                        // 6. Save Gemini generated title and category
                        savedComplaint.setTitle(
                                        aiAnalysisResult.title());
                        if (aiAnalysisResult.category() != null) {
                                savedComplaint.setCategory(
                                                aiAnalysisResult.category().name());
                        }

                        complaintRepository.save(savedComplaint);

                        // 7. Get saved AI analysis
                        ComplaintAnalysis analysis = complaintAnalysisRepository
                                        .findByComplaintId(
                                                        savedComplaint.getId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Complaint analysis not found"));

                        // 8. Detect duplicate complaints
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
                         * All candidates undergo Stage-2 Gemini verification
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

                                // Get matched complaint's latest image (may be null if text-only)
                                ComplaintImage matchedImage = complaintImageRepository
                                                .findTopByComplaintIdOrderByCreatedAtDesc(
                                                                duplicateResult
                                                                                .matchedComplaint()
                                                                                .getId())
                                                .orElse(null);

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

                // 9. Return response
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
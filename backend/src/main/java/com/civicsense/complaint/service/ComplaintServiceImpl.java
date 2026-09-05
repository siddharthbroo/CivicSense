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
import com.civicsense.complaint.service.image.ImageKitService;
import com.civicsense.complaint.service.image.ImageUploadResult;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
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

    @Override
    @Transactional
    public CreateComplaintResponse createComplaint(
            UUID userId,
            CreateComplaintRequest request,
            MultipartFile image
    ) {

        // 1. Image is mandatory
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Complaint image is required"
            );
        }

        // 2. Find logged-in user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // 3. Create complaint
        Complaint complaint = new Complaint();

        complaint.setUser(user);
        complaint.setDescription(request.description());
        complaint.setLatitude(request.latitude());
        complaint.setLongitude(request.longitude());
        complaint.setAddress(request.address());
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        // 4. Save complaint
        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // 5. Upload image to ImageKit
        ImageUploadResult uploadResult =
                imageKitService.uploadImage(image);

        // 6. Create image record
        ComplaintImage complaintImage =
                new ComplaintImage();

        complaintImage.setComplaint(savedComplaint);
        complaintImage.setImageUrl(
                uploadResult.imageUrl()
        );
        complaintImage.setStorageKey(
                uploadResult.storageKey()
        );
        complaintImage.setContentType(
                image.getContentType()
        );

        // 7. Save image reference
        complaintImageRepository.save(complaintImage);

        // 8. Analyze complaint using Gemini
        AiAnalysisResult aiAnalysisResult =
                aiAnalysisService.analyze(
                        savedComplaint,
                        complaintImage
                );

        if (aiAnalysisResult != null) {

            // 9. Save Gemini generated title
            savedComplaint.setTitle(
                    aiAnalysisResult.title()
            );

            complaintRepository.save(savedComplaint);

            // 10. Get saved AI analysis
            ComplaintAnalysis analysis =
                    complaintAnalysisRepository
                            .findByComplaintId(
                                    savedComplaint.getId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Complaint analysis not found"
                                    )
                            );

            // 11. Detect duplicate complaints
            ComplaintDuplicateDetectionService.DuplicateDetectionResult duplicateResult =
                    duplicateDetectionService.detectDuplicates(
                            savedComplaint,
                            analysis
                    );

            System.out.println(
                    "===== DUPLICATE DETECTION ====="
            );

            System.out.println(
                    "Decision = "
                            + duplicateResult.decision()
            );

            System.out.println(
                    "Score = "
                            + duplicateResult.score()
            );

            if (duplicateResult.decision()
                    == ComplaintDuplicateScoringService.DuplicateDecision.DUPLICATE
                    && duplicateResult.matchedComplaint() != null) {

                complaintClusterService.assignToExistingCluster(
                        savedComplaint,
                        duplicateResult.matchedComplaint()
                );

                System.out.println(
                        "Complaint assigned to existing cluster"
                );
            }

            else if (duplicateResult.decision()
                    == ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE) {

                complaintClusterService.createNewCluster(
                        savedComplaint,
                        analysis.getCategory()
                );

                System.out.println(
                        "New complaint cluster created"
                );
            }

            else if (duplicateResult.decision()
                    == ComplaintDuplicateScoringService.DuplicateDecision.POTENTIAL_DUPLICATE) {

                System.out.println(
                        "Complaint marked as potential duplicate"
                );
            }

            System.out.println(
                    "==============================="
            );
        }

        // 12. Return response
        return new CreateComplaintResponse(
                savedComplaint.getId(),
                savedComplaint.getStatus(),
                savedComplaint.getCreatedAt(),
                "Complaint submitted successfully"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(
            UUID userId
    ) {

        List<Complaint> complaints =
                complaintRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userId
                        );

        return complaints.stream()
                .map(complaint -> {

                    String imageUrl =
                            complaintImageRepository
                                    .findTopByComplaintIdOrderByCreatedAtDesc(
                                            complaint.getId()
                                    )
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
                            imageUrl
                    );
                })
                .toList();
    }
}
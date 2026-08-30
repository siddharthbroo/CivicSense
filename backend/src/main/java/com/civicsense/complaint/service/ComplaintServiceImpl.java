package com.civicsense.complaint.service;

import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintImage;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.service.image.ImageKitService;
import com.civicsense.complaint.service.image.ImageUploadResult;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.civicsense.complaint.service.ai.AiAnalysisService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final UserRepository userRepository;
    private final ImageKitService imageKitService;
    private final AiAnalysisService aiAnalysisService;

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

        aiAnalysisService.analyze(
                savedComplaint,
                complaintImage
        );

        // 8. Return response
        return new CreateComplaintResponse(
                savedComplaint.getId(),
                savedComplaint.getStatus(),
                savedComplaint.getCreatedAt(),
                "Complaint submitted successfully"
        );
    }
}
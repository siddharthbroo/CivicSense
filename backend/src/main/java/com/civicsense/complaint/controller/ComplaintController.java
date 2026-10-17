package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.ComplaintResponse;
import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import com.civicsense.complaint.service.ComplaintService;
import com.civicsense.complaint.service.geo.LocationValidationResult;
import com.civicsense.complaint.service.geo.LocationValidationService;
import com.civicsense.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

        private final ComplaintService complaintService;
        private final LocationValidationService locationValidationService;
        private final com.civicsense.complaint.service.image.ImageModerationService imageModerationService;

        @PostMapping("/validate-image")
        public ResponseEntity<com.civicsense.complaint.service.image.ImageModerationResult> validateImage(
                        @RequestParam("image") MultipartFile image,
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "description", required = false) String description) {

                com.civicsense.complaint.service.image.ImageModerationResult result =
                                imageModerationService.validateAndModerate(image, category, description);

                return ResponseEntity.ok(result);
        }

        @GetMapping("/validate-location")
        public ResponseEntity<LocationValidationResult> validateLocation(
                        @RequestParam("latitude") Double latitude,
                        @RequestParam("longitude") Double longitude,
                        @RequestParam(value = "category", required = false) String category) {

                LocationValidationResult result = locationValidationService.validateIncidentLocation(
                                latitude,
                                longitude,
                                category);

                return ResponseEntity.ok(result);
        }

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<CreateComplaintResponse> createComplaint(
                        @AuthenticationPrincipal User user,
                        @Valid @ModelAttribute CreateComplaintRequest request,
                        @RequestParam(value = "image", required = false) MultipartFile image) {

                CreateComplaintResponse response = complaintService.createComplaint(
                                user.getId(),
                                request,
                                image);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @GetMapping("/my")
        public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
                        @AuthenticationPrincipal User user) {

                List<ComplaintResponse> complaints = complaintService.getMyComplaints(user.getId());

                return ResponseEntity.ok(complaints);
        }
}
package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.ComplaintResponse;
import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import com.civicsense.complaint.service.ComplaintService;
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

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CreateComplaintResponse> createComplaint(

            @AuthenticationPrincipal User user,

            @Valid @ModelAttribute CreateComplaintRequest request,

            @RequestParam("image") MultipartFile image
    ) {

        CreateComplaintResponse response =
                complaintService.createComplaint(
                        user.getId(),
                        request,
                        image
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal User user
    ) {

        List<ComplaintResponse> complaints =
                complaintService.getMyComplaints(user.getId());

        return ResponseEntity.ok(complaints);
    }
}
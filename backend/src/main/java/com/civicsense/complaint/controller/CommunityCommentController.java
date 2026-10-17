package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.community.CommunityCommentResponse;
import com.civicsense.complaint.dto.community.CreateCommentRequest;
import com.civicsense.complaint.dto.community.UpdateCommentRequest;
import com.civicsense.complaint.service.community.ComplaintCommentService;
import com.civicsense.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityCommentController {

    private final ComplaintCommentService complaintCommentService;

    @PostMapping("/complaints/{complaintId}/comments")
    public ResponseEntity<CommunityCommentResponse> createComment(
            @PathVariable UUID complaintId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CommunityCommentResponse response = complaintCommentService.createComment(complaintId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/complaints/{complaintId}/comments")
    public ResponseEntity<Page<CommunityCommentResponse>> getComments(
            @PathVariable UUID complaintId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        int boundedSize = Math.min(Math.max(1, size), 50);
        int boundedPage = Math.max(0, page);
        Page<CommunityCommentResponse> response = complaintCommentService.getComments(
                complaintId,
                PageRequest.of(boundedPage, boundedSize),
                user
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommunityCommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CommunityCommentResponse response = complaintCommentService.updateComment(commentId, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        complaintCommentService.deleteComment(commentId, user);
        return ResponseEntity.noContent().build();
    }
}


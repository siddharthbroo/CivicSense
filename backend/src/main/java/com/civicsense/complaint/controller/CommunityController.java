package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.community.CommunityComplaintResponse;
import com.civicsense.complaint.dto.community.ComplaintCommunityResponse;
import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.service.community.CitizenCorroborationService;
import com.civicsense.complaint.service.community.ComplaintCommunityService;
import com.civicsense.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final ComplaintCommunityService complaintCommunityService;
    private final CitizenCorroborationService citizenCorroborationService;

    @GetMapping("/complaints")
    public ResponseEntity<List<CommunityComplaintResponse>> getCommunityFeed(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false, defaultValue = "RECENT") String sort,
            @AuthenticationPrincipal User user
    ) {
        UUID userId = (user != null) ? user.getId() : null;
        List<CommunityComplaintResponse> feed = complaintCommunityService.getCommunityFeed(status, sort, userId);
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/complaints/nearby")
    public ResponseEntity<List<CommunityComplaintResponse>> getNearbyComplaints(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radiusKm,
            @AuthenticationPrincipal User user
    ) {
        UUID userId = (user != null) ? user.getId() : null;
        List<CommunityComplaintResponse> nearby = complaintCommunityService.getNearbyCommunityComplaints(latitude, longitude, radiusKm, userId);
        return ResponseEntity.ok(nearby);
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<CommunityComplaintResponse> getCommunityComplaint(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        UUID userId = (user != null) ? user.getId() : null;
        CommunityComplaintResponse response = complaintCommunityService.getCommunityComplaint(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints/{id}/support")
    public ResponseEntity<ComplaintCommunityResponse> supportComplaint(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ComplaintCommunityResponse response = complaintCommunityService.supportComplaint(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/complaints/{id}/support")
    public ResponseEntity<ComplaintCommunityResponse> removeSupport(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ComplaintCommunityResponse response = complaintCommunityService.removeSupport(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/complaints/{id}/corroboration")
    public ResponseEntity<CorroborationIntelligence> getCorroboration(
            @PathVariable UUID id
    ) {
        CorroborationIntelligence intelligence = citizenCorroborationService.getCorroboration(id);
        return ResponseEntity.ok(intelligence);
    }
}


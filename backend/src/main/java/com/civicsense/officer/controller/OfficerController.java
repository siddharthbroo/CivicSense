package com.civicsense.officer.controller;

import com.civicsense.officer.dto.OfficerProfileResponse;
import com.civicsense.officer.dto.ProvisionOfficerRequest;
import com.civicsense.officer.dto.UpdateOfficerVerificationRequest;
import com.civicsense.officer.entity.OfficerVerificationStatus;
import com.civicsense.officer.service.OfficerProfileService;
import com.civicsense.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for officer profile provisioning, verification lifecycle, and self profile retrieval.
 *
 * <p>Administrative provisioning and status mutation endpoints require ADMIN role.
 * Officer self endpoints require OFFICER role.
 */
@RestController
@RequiredArgsConstructor
public class OfficerController {

    private final OfficerProfileService officerProfileService;

    // =========================================================================
    // Administrative Endpoints
    // =========================================================================

    @PostMapping("/api/v1/admin/officers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficerProfileResponse> provisionOfficer(
            @Valid @RequestBody ProvisionOfficerRequest request
    ) {
        OfficerProfileResponse response = officerProfileService.provisionOfficer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/admin/officers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfficerProfileResponse>> getOfficers(
            @RequestParam(required = false) String municipality,
            @RequestParam(required = false) OfficerVerificationStatus status
    ) {
        return ResponseEntity.ok(officerProfileService.getOfficers(municipality, status));
    }

    @GetMapping("/api/v1/admin/officers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficerProfileResponse> getOfficerById(@PathVariable UUID id) {
        return ResponseEntity.ok(officerProfileService.getOfficerProfileById(id));
    }

    @PatchMapping("/api/v1/admin/officers/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficerProfileResponse> updateVerificationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfficerVerificationRequest request
    ) {
        return ResponseEntity.ok(officerProfileService.updateVerificationStatus(id, request));
    }

    // =========================================================================
    // Officer Self Endpoint
    // =========================================================================

    @GetMapping("/api/v1/officers/me")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<OfficerProfileResponse> getMyOfficerProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(officerProfileService.getOfficerProfileByUserId(currentUser.getId()));
    }
}


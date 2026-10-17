package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.geo.*;
import com.civicsense.complaint.service.geo.AdministrativeGeographyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing municipal administrative geography (Zones and Wards).
 *
 * <p>Read operations are available to authenticated users.
 * Administrative write/mutation operations require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/geography")
@RequiredArgsConstructor
public class AdministrativeGeographyController {

    private final AdministrativeGeographyService geographyService;

    // =========================================================================
    // Zone Endpoints
    // =========================================================================

    @GetMapping("/zones")
    public ResponseEntity<List<ZoneResponse>> getZones(
            @RequestParam(required = false) String municipality,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ResponseEntity.ok(geographyService.getZones(municipality, activeOnly));
    }

    @GetMapping("/zones/{id}")
    public ResponseEntity<ZoneResponse> getZoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getZoneById(id));
    }

    @PostMapping("/zones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody CreateZoneRequest request) {
        ZoneResponse response = geographyService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/zones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> updateZone(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateZoneRequest request
    ) {
        return ResponseEntity.ok(geographyService.updateZone(id, request));
    }

    // =========================================================================
    // Ward Endpoints
    // =========================================================================

    @GetMapping("/wards")
    public ResponseEntity<List<WardResponse>> getWards(
            @RequestParam(required = false) String municipality,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return ResponseEntity.ok(geographyService.getWards(municipality, zoneId, activeOnly));
    }

    @GetMapping("/wards/{id}")
    public ResponseEntity<WardResponse> getWardById(@PathVariable UUID id) {
        return ResponseEntity.ok(geographyService.getWardById(id));
    }

    @PostMapping("/wards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody CreateWardRequest request) {
        WardResponse response = geographyService.createWard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/wards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WardResponse> updateWard(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWardRequest request
    ) {
        return ResponseEntity.ok(geographyService.updateWard(id, request));
    }
}


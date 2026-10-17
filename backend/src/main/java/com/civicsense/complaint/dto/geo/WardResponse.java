package com.civicsense.complaint.dto.geo;

import com.civicsense.complaint.entity.geo.BoundaryProvenance;

import java.time.LocalDateTime;
import java.util.UUID;

public record WardResponse(
        UUID id,
        UUID zoneId,
        String zoneName,
        String name,
        String wardNumber,
        String municipality,
        BoundaryProvenance boundaryProvenance,
        String dataSource,
        boolean active,
        Double minLatitude,
        Double maxLatitude,
        Double minLongitude,
        Double maxLongitude,
        Double centerLatitude,
        Double centerLongitude,
        UUID assignedOfficerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}


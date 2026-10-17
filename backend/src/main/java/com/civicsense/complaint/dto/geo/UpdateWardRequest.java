package com.civicsense.complaint.dto.geo;

import com.civicsense.complaint.entity.geo.BoundaryProvenance;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateWardRequest(
        UUID zoneId,

        @Size(max = 100, message = "Ward name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Ward number must not exceed 50 characters")
        String wardNumber,

        String municipality,
        BoundaryProvenance boundaryProvenance,
        String dataSource,
        Boolean active,

        Double minLatitude,
        Double maxLatitude,
        Double minLongitude,
        Double maxLongitude,
        Double centerLatitude,
        Double centerLongitude,

        Boolean forceBoundaryOverwrite
) {}


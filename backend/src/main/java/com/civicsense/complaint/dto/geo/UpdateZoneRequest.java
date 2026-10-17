package com.civicsense.complaint.dto.geo;

import jakarta.validation.constraints.Size;

public record UpdateZoneRequest(
        @Size(max = 100, message = "Zone name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Zone code must not exceed 50 characters")
        String code,

        String municipality,
        String region,
        String dataSource,
        Boolean active
) {}


package com.civicsense.complaint.dto.geo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateZoneRequest(
        @NotBlank(message = "Zone name is required")
        @Size(max = 100, message = "Zone name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Zone code is required")
        @Size(max = 50, message = "Zone code must not exceed 50 characters")
        String code,

        String municipality,
        String region,
        String dataSource
) {}


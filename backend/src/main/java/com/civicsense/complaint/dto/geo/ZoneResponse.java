package com.civicsense.complaint.dto.geo;

import java.time.LocalDateTime;
import java.util.UUID;

public record ZoneResponse(
        UUID id,
        String name,
        String code,
        String municipality,
        String region,
        String dataSource,
        boolean active,
        int wardCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}


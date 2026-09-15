package com.civicsense.complaint.dto.community;

import java.time.LocalDateTime;
import java.util.UUID;

public record CorroborationIntelligence(
        UUID complaintId,
        UUID clusterId,
        int totalReports,
        int uniqueCitizens,
        long supportCount,
        long uniqueSupporters,
        boolean recurring,
        int recurrenceCount,
        LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt,
        double corroborationScore
) {
}


package com.civicsense.complaint.dto.community;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComplaintCommunityResponse(
        UUID complaintId,
        long supportCount,
        boolean supportedByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


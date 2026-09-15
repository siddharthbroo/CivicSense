package com.civicsense.complaint.dto.community;

import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.complaint.entity.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityComplaintResponse(
        UUID complaintId,
        String imageUrl,
        String title,
        String description,
        String category,
        String address,
        Double latitude,
        Double longitude,
        ComplaintPriority priority,
        ComplaintStatus status,
        long supportCount,
        long commentCount,
        int totalReports,
        int uniqueCitizens,
        boolean recurring,
        int recurrenceCount,
        LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt,
        boolean supportedByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


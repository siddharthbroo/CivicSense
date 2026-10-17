package com.civicsense.complaint.dto.community;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommunityCommentResponse(
        UUID id,
        UUID complaintId,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean editableByCurrentUser,
        boolean deletableByCurrentUser
) {
}


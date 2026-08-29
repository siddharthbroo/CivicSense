package com.civicsense.complaint.dto;

import com.civicsense.complaint.entity.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateComplaintResponse(

        UUID complaintId,

        ComplaintStatus status,

        LocalDateTime createdAt,

        String message
) {
}
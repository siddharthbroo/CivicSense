package com.civicsense.complaint.dto;

import com.civicsense.complaint.entity.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComplaintResponse(
                UUID id,
                String description,
                Double latitude,
                Double longitude,
                String address,
                ComplaintStatus status,
                LocalDateTime createdAt,
                String imageUrl) {
}

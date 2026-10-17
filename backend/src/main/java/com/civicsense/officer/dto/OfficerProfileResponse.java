package com.civicsense.officer.dto;

import com.civicsense.officer.entity.OfficerVerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfficerProfileResponse(
        UUID id,
        UUID userId,
        String userName,
        String userMobile,
        String userEmail,
        String employeeCode,
        String municipality,
        String designation,
        String department,
        String governmentEmail,
        OfficerVerificationStatus verificationStatus,
        boolean active,
        boolean operationallyAuthorized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}


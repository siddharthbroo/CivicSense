package com.civicsense.officer.dto;

import com.civicsense.officer.entity.OfficerVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOfficerVerificationRequest(
        @NotNull(message = "Target verification status is required")
        OfficerVerificationStatus targetStatus,

        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason
) {}


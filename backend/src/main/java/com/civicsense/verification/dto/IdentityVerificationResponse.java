package com.civicsense.verification.dto;

import com.civicsense.verification.entity.OCRStatus;
import com.civicsense.verification.entity.VerificationStatus;

import java.util.UUID;

public record IdentityVerificationResponse(
        UUID verificationId,
        OCRStatus ocrStatus,
        VerificationStatus verificationStatus,
        String message
) {
}
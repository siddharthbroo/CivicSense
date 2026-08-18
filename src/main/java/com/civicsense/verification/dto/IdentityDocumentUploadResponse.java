package com.civicsense.verification.dto;

import com.civicsense.verification.entity.OCRStatus;

import java.time.LocalDate;
import java.util.UUID;

public record IdentityDocumentUploadResponse(
        UUID verificationId,
        String documentType,
        OCRStatus ocrStatus,
        String extractedName,
        LocalDate extractedDob,
        String extractedGender
) {
}
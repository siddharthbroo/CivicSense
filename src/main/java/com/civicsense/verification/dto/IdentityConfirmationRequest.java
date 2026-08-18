package com.civicsense.verification.dto;

import java.time.LocalDate;
import java.util.UUID;

public record IdentityConfirmationRequest(
        UUID verificationId,
        String name,
        LocalDate dateOfBirth,
        String gender
) {
}
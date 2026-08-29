package com.civicsense.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record IdentityConfirmationRequest(

        @NotNull(message = "Verification ID is required")
        UUID verificationId,

        @NotBlank(message = "Name is required")
        @Size(
                min = 2,
                max = 100,
                message = "Name must be between 2 and 100 characters"
        )
        String name,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotBlank(message = "Gender is required")
        String gender
) {
}
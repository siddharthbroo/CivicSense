package com.civicsense.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SendOtpRequest(

        @NotNull(message = "Identity verification ID is required")
        UUID identityVerificationId,

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Mobile number must be a valid 10-digit Indian mobile number"
        )
        String mobileNumber
) {
}
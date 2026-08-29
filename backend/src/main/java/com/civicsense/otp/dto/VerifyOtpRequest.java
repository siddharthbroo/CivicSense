package com.civicsense.otp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(

        @NotNull(message = "OTP verification ID is required")
        UUID otpVerificationId,

        @NotBlank(message = "OTP is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "OTP must be exactly 6 digits"
        )
        String otp
) {
}
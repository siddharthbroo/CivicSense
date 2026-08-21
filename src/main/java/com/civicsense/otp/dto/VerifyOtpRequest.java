package com.civicsense.otp.dto;

import java.util.UUID;

public record VerifyOtpRequest(
        UUID otpVerificationId,
        String otp
) {
}
package com.civicsense.otp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SendOtpResponse(
        UUID otpVerificationId,
        LocalDateTime expiresAt,
        String message
) {
}
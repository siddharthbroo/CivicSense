package com.civicsense.otp.dto;

public record VerifyOtpResponse(
        boolean verified,
        String message
) {
}
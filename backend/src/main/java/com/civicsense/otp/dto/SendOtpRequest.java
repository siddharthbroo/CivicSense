package com.civicsense.otp.dto;

import java.util.UUID;

public record SendOtpRequest(
        UUID identityVerificationId,
        String mobileNumber
) {
}
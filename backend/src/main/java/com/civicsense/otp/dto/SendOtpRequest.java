package com.civicsense.otp.dto;

import com.civicsense.otp.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Mobile number must be a valid 10-digit Indian mobile number"
        )
        String mobileNumber,

        OtpPurpose purpose
) {
    /** Convenience constructor: defaults purpose to REGISTRATION. */
    public SendOtpRequest(String mobileNumber) {
        this(mobileNumber, OtpPurpose.REGISTRATION);
    }
}
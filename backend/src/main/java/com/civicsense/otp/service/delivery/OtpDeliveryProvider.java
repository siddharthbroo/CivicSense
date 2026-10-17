package com.civicsense.otp.service.delivery;

import java.time.LocalDateTime;

/**
 * Abstraction for OTP delivery. Implementations may log to console (development)
 * or send a real SMS (production). OtpServiceImpl depends only on this interface.
 */
public interface OtpDeliveryProvider {

    /**
     * Deliver the given OTP to the specified mobile number.
     *
     * @param mobileNumber the recipient mobile number
     * @param otp          the plaintext 6-digit OTP to deliver
     * @param expiresAt    the expiry timestamp for informational purposes
     */
    void sendOtp(String mobileNumber, String otp, java.time.LocalDateTime expiresAt);
}
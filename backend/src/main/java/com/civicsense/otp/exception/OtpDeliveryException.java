package com.civicsense.otp.exception;

/**
 * Thrown when the OtpDeliveryProvider fails to deliver an OTP
 * (e.g. SMS gateway error, network failure).
 */
public class OtpDeliveryException extends RuntimeException {

    public OtpDeliveryException(String message) {
        super(message);
    }

    public OtpDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
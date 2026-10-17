package com.civicsense.otp.service.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SMS OTP delivery provider stub - active when civicsense.otp.delivery-provider=sms.
 * Replace the body of sendOtp() with a real SMS gateway call (MSG91, Twilio, etc.)
 * without touching any other class.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "civicsense.otp.delivery-provider",
        havingValue = "sms"
)
public class SmsOtpDeliveryProvider implements OtpDeliveryProvider {

    @Override
    public void sendOtp(String mobileNumber, String otp, LocalDateTime expiresAt) {
        // TODO: integrate a real SMS gateway here.
        log.warn("[SMS-STUB] SMS delivery not yet implemented. Mobile: {}", mobileNumber);
        throw new UnsupportedOperationException("SMS provider is not yet configured.");
    }
}
package com.civicsense.otp.service.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Development OTP delivery provider - logs the OTP to the application console.
 * Active when civicsense.otp.delivery-provider=development (or property is absent).
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "civicsense.otp.delivery-provider",
        havingValue = "development",
        matchIfMissing = true
)
public class DevelopmentOtpDeliveryProvider implements OtpDeliveryProvider {

    @Override
    public void sendOtp(String mobileNumber, String otp, LocalDateTime expiresAt) {
        log.info("================================================================");
        log.info("[DEV OTP] Mobile: {} | OTP: {} | Expires: {}", mobileNumber, otp, expiresAt);
        log.info("================================================================");
    }
}
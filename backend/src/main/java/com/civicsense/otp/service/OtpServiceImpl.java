package com.civicsense.otp.service;

import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.otp.dto.SendOtpRequest;
import com.civicsense.otp.dto.SendOtpResponse;
import com.civicsense.otp.dto.VerifyOtpRequest;
import com.civicsense.otp.dto.VerifyOtpResponse;
import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import com.civicsense.otp.repository.OtpVerificationRepository;
import com.civicsense.otp.service.delivery.OtpDeliveryProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_MIN = 100000;
    private static final int OTP_MAX = 999999;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpHasher otpHasher;
    private final OtpDeliveryProvider otpDeliveryProvider;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request) {

        OtpPurpose purpose = request.purpose() != null
                ? request.purpose()
                : OtpPurpose.REGISTRATION;

        /*
         * Invalidate all previously active OTPs for this mobile + purpose.
         * This handles the "Resend OTP" case — the old OTP becomes unusable.
         */
        otpVerificationRepository.updateStatusForActiveOtps(
                request.mobileNumber(),
                purpose,
                OtpStatus.PENDING,
                OtpStatus.EXPIRED
        );

        // Generate secure 6-digit OTP
        String otp = generateOtp();

        // Hash OTP before storing — never store plaintext
        String otpHash = otpHasher.hash(otp);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setMobileNumber(request.mobileNumber());
        otpVerification.setOtpHash(otpHash);
        otpVerification.setPurpose(purpose);
        otpVerification.setStatus(OtpStatus.PENDING);
        otpVerification.setExpiresAt(expiresAt);
        otpVerification.setAttemptCount(0);

        OtpVerification savedOtp = otpVerificationRepository.save(otpVerification);

        // Deliver OTP via the configured provider (development=log, sms=gateway)
        otpDeliveryProvider.sendOtp(request.mobileNumber(), otp, expiresAt);

        return new SendOtpResponse(
                savedOtp.getId(),
                savedOtp.getExpiresAt(),
                "OTP sent successfully"
        );
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {

        OtpVerification otpVerification =
                otpVerificationRepository.findById(request.otpVerificationId())
                        .orElseThrow(() -> new ResourceNotFoundException("OTP verification not found"));

        // OTP must still be pending
        if (otpVerification.getStatus() != OtpStatus.PENDING) {
            return new VerifyOtpResponse(false, "OTP is no longer active");
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(otpVerification.getExpiresAt())) {
            otpVerification.setStatus(OtpStatus.EXPIRED);
            otpVerificationRepository.save(otpVerification);
            return new VerifyOtpResponse(false, "OTP has expired");
        }

        // Check maximum attempts
        if (otpVerification.getAttemptCount() >= MAX_ATTEMPTS) {
            otpVerification.setStatus(OtpStatus.FAILED);
            otpVerificationRepository.save(otpVerification);
            return new VerifyOtpResponse(false, "Maximum OTP attempts exceeded");
        }

        // Increment attempt count
        otpVerification.setAttemptCount(otpVerification.getAttemptCount() + 1);

        // Compare entered OTP with stored hash
        boolean matches = otpHasher.matches(request.otp(), otpVerification.getOtpHash());

        if (!matches) {
            if (otpVerification.getAttemptCount() >= MAX_ATTEMPTS) {
                otpVerification.setStatus(OtpStatus.FAILED);
            }
            otpVerificationRepository.save(otpVerification);
            return new VerifyOtpResponse(false, "Invalid OTP");
        }

        // Correct OTP — mark as verified
        otpVerification.setStatus(OtpStatus.VERIFIED);
        otpVerification.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otpVerification);

        return new VerifyOtpResponse(true, "OTP verified successfully");
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(OTP_MAX - OTP_MIN + 1) + OTP_MIN;
        return String.valueOf(otp);
    }
}

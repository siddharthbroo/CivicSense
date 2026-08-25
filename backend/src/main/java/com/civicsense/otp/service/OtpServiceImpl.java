package com.civicsense.otp.service;

import com.civicsense.otp.dto.SendOtpRequest;
import com.civicsense.otp.dto.SendOtpResponse;
import com.civicsense.otp.dto.VerifyOtpRequest;
import com.civicsense.otp.dto.VerifyOtpResponse;
import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import com.civicsense.otp.repository.OtpVerificationRepository;
import com.civicsense.verification.entity.IdentityVerification;
import com.civicsense.verification.entity.VerificationStatus;
import com.civicsense.verification.repository.IdentityVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int OTP_MIN = 100000;
    private static final int OTP_MAX = 999999;
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final OtpVerificationRepository otpVerificationRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final OtpHasher otpHasher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public SendOtpResponse sendOtp(SendOtpRequest request) {

        // 1. Find the identity verification process
        IdentityVerification verification =
                identityVerificationRepository.findById(
                        request.identityVerificationId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Identity verification not found"
                        )
                );

        // 2. User must have confirmed the identity details first
        if (verification.getVerificationStatus()
                != VerificationStatus.USER_CONFIRMED) {

            throw new IllegalStateException(
                    "Identity details must be confirmed before requesting OTP"
            );
        }

        // 3. Generate secure 6-digit OTP
        String otp = generateOtp();

        // 4. Hash OTP before storing it
        String otpHash = otpHasher.hash(otp);

        // 5. Set expiry time
        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // 6. Create OTP record
        OtpVerification otpVerification =
                new OtpVerification();

        otpVerification.setIdentityVerification(verification);
        otpVerification.setMobileNumber(request.mobileNumber());
        otpVerification.setOtpHash(otpHash);
        otpVerification.setPurpose(OtpPurpose.REGISTRATION);
        otpVerification.setStatus(OtpStatus.PENDING);
        otpVerification.setExpiresAt(expiresAt);
        otpVerification.setAttemptCount(0);

        // 7. Save OTP
        OtpVerification savedOtp =
                otpVerificationRepository.save(otpVerification);

        // 8. Development only
        System.out.println(
                "========================================"
        );
        System.out.println(
                "Development OTP: " + otp
        );
        System.out.println(
                "OTP Verification ID: " + savedOtp.getId()
        );
        System.out.println(
                "Expires At: " + expiresAt
        );
        System.out.println(
                "========================================"
        );

        // 9. Never return the actual OTP in API response
        return new SendOtpResponse(
                savedOtp.getId(),
                savedOtp.getExpiresAt(),
                "OTP generated successfully"
        );
    }

    @Override
    public VerifyOtpResponse verifyOtp(
            VerifyOtpRequest request
    ) {

        // 1. Find the OTP verification record
        OtpVerification otpVerification =
                otpVerificationRepository.findById(
                        request.otpVerificationId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "OTP verification not found"
                        )
                );

        // 2. OTP must still be pending
        if (otpVerification.getStatus() != OtpStatus.PENDING) {

            return new VerifyOtpResponse(
                    false,
                    "OTP is no longer active"
            );
        }

        // 3. Check whether OTP has expired
        if (LocalDateTime.now()
                .isAfter(otpVerification.getExpiresAt())) {

            otpVerification.setStatus(OtpStatus.EXPIRED);

            otpVerificationRepository.save(otpVerification);

            return new VerifyOtpResponse(
                    false,
                    "OTP has expired"
            );
        }

        // 4. Check maximum attempts
        if (otpVerification.getAttemptCount() >= 5) {

            otpVerification.setStatus(OtpStatus.FAILED);

            otpVerificationRepository.save(otpVerification);

            return new VerifyOtpResponse(
                    false,
                    "Maximum OTP attempts exceeded"
            );
        }

        // 5. Increase attempt count
        otpVerification.setAttemptCount(
                otpVerification.getAttemptCount() + 1
        );

        // 6. Compare entered OTP with stored hash
        boolean matches =
                otpHasher.matches(
                        request.otp(),
                        otpVerification.getOtpHash()
                );

        // 7. Wrong OTP
        if (!matches) {

            if (otpVerification.getAttemptCount() >= 5) {
                otpVerification.setStatus(OtpStatus.FAILED);
            }

            otpVerificationRepository.save(otpVerification);

            return new VerifyOtpResponse(
                    false,
                    "Invalid OTP"
            );
        }

        // 8. Correct OTP
        otpVerification.setStatus(OtpStatus.VERIFIED);
        otpVerification.setVerifiedAt(LocalDateTime.now());

        otpVerificationRepository.save(otpVerification);

        return new VerifyOtpResponse(
                true,
                "OTP verified successfully"
        );
    }

    private String generateOtp() {

        int otp = secureRandom.nextInt(
                OTP_MAX - OTP_MIN + 1
        ) + OTP_MIN;

        return String.valueOf(otp);
    }
}
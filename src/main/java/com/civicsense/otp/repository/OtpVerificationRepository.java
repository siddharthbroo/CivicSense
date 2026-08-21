package com.civicsense.otp.repository;

import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification>
    findTopByIdentityVerificationIdAndMobileNumberAndPurposeAndStatusOrderByCreatedAtDesc(
            UUID identityVerificationId,
            String mobileNumber,
            OtpPurpose purpose,
            OtpStatus status
    );
}
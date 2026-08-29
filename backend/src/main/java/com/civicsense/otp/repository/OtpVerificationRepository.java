package com.civicsense.otp.repository;

import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("""
            UPDATE OtpVerification o
            SET o.status = :newStatus
            WHERE o.identityVerification.id = :identityVerificationId
            AND o.mobileNumber = :mobileNumber
            AND o.purpose = :purpose
            AND o.status = :currentStatus
            """)
    int updateStatusForActiveOtps(
            @Param("identityVerificationId") UUID identityVerificationId,
            @Param("mobileNumber") String mobileNumber,
            @Param("purpose") OtpPurpose purpose,
            @Param("currentStatus") OtpStatus currentStatus,
            @Param("newStatus") OtpStatus newStatus
    );
}
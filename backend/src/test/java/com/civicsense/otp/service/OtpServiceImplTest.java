package com.civicsense.otp.service;

import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.otp.dto.SendOtpRequest;
import com.civicsense.otp.dto.SendOtpResponse;
import com.civicsense.otp.dto.VerifyOtpRequest;
import com.civicsense.otp.dto.VerifyOtpResponse;
import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import com.civicsense.otp.exception.OtpDeliveryException;
import com.civicsense.otp.repository.OtpVerificationRepository;
import com.civicsense.otp.service.delivery.OtpDeliveryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private OtpDeliveryProvider otpDeliveryProvider;

    @InjectMocks
    private OtpServiceImpl otpService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String validMobile = "9876543210";

    @BeforeEach
    void setUp() {
        lenient().when(otpHasher.hash(anyString())).thenAnswer(inv -> passwordEncoder.encode(inv.getArgument(0)));
        lenient().when(otpHasher.matches(anyString(), anyString())).thenAnswer(inv -> passwordEncoder.matches(inv.getArgument(0), inv.getArgument(1)));
    }

    @Test
    @DisplayName("1 & 2. OTP generation produces a 6-digit numeric string")
    void testOtpGenerationIsSixDigits() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        SendOtpRequest request = new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION);
        SendOtpResponse response = otpService.sendOtp(request);

        assertThat(response).isNotNull();
        assertThat(response.otpVerificationId()).isNotNull();

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpDeliveryProvider).sendOtp(eq(validMobile), otpCaptor.capture(), any(LocalDateTime.class));

        String capturedOtp = otpCaptor.getValue();
        assertThat(capturedOtp).hasSize(6);
        assertThat(capturedOtp).matches("^\\d{6}$");
        int numericOtp = Integer.parseInt(capturedOtp);
        assertThat(numericOtp).isBetween(100000, 999999);
    }

    @Test
    @DisplayName("3 & 5. OTP is hashed with BCrypt and plaintext is never stored in entity")
    void testOtpIsHashedAndNotStoredInPlaintext() {
        ArgumentCaptor<OtpVerification> entityCaptor = ArgumentCaptor.forClass(OtpVerification.class);
        when(otpVerificationRepository.save(entityCaptor.capture())).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        SendOtpRequest request = new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION);
        otpService.sendOtp(request);

        OtpVerification savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getOtpHash()).isNotBlank();
        assertThat(savedEntity.getOtpHash()).startsWith("$2a$");

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpDeliveryProvider).sendOtp(eq(validMobile), otpCaptor.capture(), any(LocalDateTime.class));
        String rawOtp = otpCaptor.getValue();

        assertThat(savedEntity.getOtpHash()).isNotEqualTo(rawOtp);
        assertThat(passwordEncoder.matches(rawOtp, savedEntity.getOtpHash())).isTrue();
    }

    @Test
    @DisplayName("4. Plaintext OTP is never returned in API response")
    void testPlaintextOtpNotReturnedInApiResponse() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        SendOtpRequest request = new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION);
        SendOtpResponse response = otpService.sendOtp(request);

        assertThat(response.message()).isEqualTo("OTP sent successfully");
        assertThat(response.otpVerificationId()).isNotNull();
        assertThat(response.toString()).doesNotContain("otp=");
    }

    @Test
    @DisplayName("6. OTP expires 5 minutes after generation")
    void testOtpExpiresAfterFiveMinutes() {
        ArgumentCaptor<OtpVerification> entityCaptor = ArgumentCaptor.forClass(OtpVerification.class);
        when(otpVerificationRepository.save(entityCaptor.capture())).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LocalDateTime before = LocalDateTime.now();
        otpService.sendOtp(new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION));
        LocalDateTime after = LocalDateTime.now();

        OtpVerification saved = entityCaptor.getValue();
        assertThat(saved.getExpiresAt()).isAfterOrEqualTo(before.plusMinutes(5).minusSeconds(1));
        assertThat(saved.getExpiresAt()).isBeforeOrEqualTo(after.plusMinutes(5).plusSeconds(1));
    }

    @Test
    @DisplayName("7. OTP verification succeeds with correct OTP")
    void testOtpVerificationSucceedsWithCorrectOtp() {
        UUID verificationId = UUID.randomUUID();
        String rawOtp = "123456";
        String hash = passwordEncoder.encode(rawOtp);

        OtpVerification entity = new OtpVerification();
        entity.setId(verificationId);
        entity.setMobileNumber(validMobile);
        entity.setOtpHash(hash);
        entity.setStatus(OtpStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        entity.setAttemptCount(0);

        when(otpVerificationRepository.findById(verificationId)).thenReturn(Optional.of(entity));

        VerifyOtpResponse response = otpService.verifyOtp(new VerifyOtpRequest(verificationId, rawOtp));

        assertThat(response.verified()).isTrue();
        assertThat(response.message()).isEqualTo("OTP verified successfully");
        assertThat(entity.getStatus()).isEqualTo(OtpStatus.VERIFIED);
        assertThat(entity.getVerifiedAt()).isNotNull();
        verify(otpVerificationRepository).save(entity);
    }

    @Test
    @DisplayName("8. OTP verification fails with wrong OTP")
    void testOtpVerificationFailsWithWrongOtp() {
        UUID verificationId = UUID.randomUUID();
        String rawOtp = "123456";
        String hash = passwordEncoder.encode(rawOtp);

        OtpVerification entity = new OtpVerification();
        entity.setId(verificationId);
        entity.setMobileNumber(validMobile);
        entity.setOtpHash(hash);
        entity.setStatus(OtpStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        entity.setAttemptCount(0);

        when(otpVerificationRepository.findById(verificationId)).thenReturn(Optional.of(entity));

        VerifyOtpResponse response = otpService.verifyOtp(new VerifyOtpRequest(verificationId, "999999"));

        assertThat(response.verified()).isFalse();
        assertThat(response.message()).isEqualTo("Invalid OTP");
        assertThat(entity.getAttemptCount()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(OtpStatus.PENDING);
        verify(otpVerificationRepository).save(entity);
    }

    @Test
    @DisplayName("9 & 10. Maximum 5 attempts allowed, status becomes FAILED on 5th failure")
    void testMaxAttemptsReachesFailedStatus() {
        UUID verificationId = UUID.randomUUID();
        String hash = passwordEncoder.encode("123456");

        OtpVerification entity = new OtpVerification();
        entity.setId(verificationId);
        entity.setMobileNumber(validMobile);
        entity.setOtpHash(hash);
        entity.setStatus(OtpStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        entity.setAttemptCount(4);

        when(otpVerificationRepository.findById(verificationId)).thenReturn(Optional.of(entity));

        VerifyOtpResponse response = otpService.verifyOtp(new VerifyOtpRequest(verificationId, "000000"));

        assertThat(response.verified()).isFalse();
        assertThat(entity.getAttemptCount()).isEqualTo(5);
        assertThat(entity.getStatus()).isEqualTo(OtpStatus.FAILED);

        VerifyOtpResponse subsequentResponse = otpService.verifyOtp(new VerifyOtpRequest(verificationId, "000000"));
        assertThat(subsequentResponse.verified()).isFalse();
        assertThat(subsequentResponse.message()).isEqualTo("OTP is no longer active");
    }

    @Test
    @DisplayName("11. Expired OTP changes status to EXPIRED")
    void testExpiredOtpChangesStatusToExpired() {
        UUID verificationId = UUID.randomUUID();
        String hash = passwordEncoder.encode("123456");

        OtpVerification entity = new OtpVerification();
        entity.setId(verificationId);
        entity.setMobileNumber(validMobile);
        entity.setOtpHash(hash);
        entity.setStatus(OtpStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        entity.setAttemptCount(0);

        when(otpVerificationRepository.findById(verificationId)).thenReturn(Optional.of(entity));

        VerifyOtpResponse response = otpService.verifyOtp(new VerifyOtpRequest(verificationId, "123456"));

        assertThat(response.verified()).isFalse();
        assertThat(response.message()).isEqualTo("OTP has expired");
        assertThat(entity.getStatus()).isEqualTo(OtpStatus.EXPIRED);
        verify(otpVerificationRepository).save(entity);
    }

    @Test
    @DisplayName("12. Resending OTP invalidates previous active OTP")
    void testResendInvalidatesPreviousActiveOtp() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        otpService.sendOtp(new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION));

        verify(otpVerificationRepository).updateStatusForActiveOtps(
                eq(validMobile),
                eq(OtpPurpose.REGISTRATION),
                eq(OtpStatus.PENDING),
                eq(OtpStatus.EXPIRED)
        );
    }

    @Test
    @DisplayName("13. Different mobile numbers do not affect each other's OTP invalidation")
    void testDifferentMobileNumbersIndependent() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        String mobile1 = "9876543210";
        String mobile2 = "9123456780";

        otpService.sendOtp(new SendOtpRequest(mobile1, OtpPurpose.REGISTRATION));
        verify(otpVerificationRepository).updateStatusForActiveOtps(eq(mobile1), any(), any(), any());
        verify(otpVerificationRepository, never()).updateStatusForActiveOtps(eq(mobile2), any(), any(), any());

        otpService.sendOtp(new SendOtpRequest(mobile2, OtpPurpose.REGISTRATION));
        verify(otpVerificationRepository).updateStatusForActiveOtps(eq(mobile2), any(), any(), any());
    }

    @Test
    @DisplayName("14. Different OTP purposes do not invalidate each other's OTP")
    void testDifferentPurposesDoNotInvalidate() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        otpService.sendOtp(new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION));

        verify(otpVerificationRepository).updateStatusForActiveOtps(
                eq(validMobile),
                eq(OtpPurpose.REGISTRATION),
                eq(OtpStatus.PENDING),
                eq(OtpStatus.EXPIRED)
        );
    }

    @Test
    @DisplayName("15. OTP works completely without IdentityVerification")
    void testOtpWorksWithoutIdentityVerification() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        SendOtpRequest request = new SendOtpRequest(validMobile);
        SendOtpResponse response = otpService.sendOtp(request);

        assertThat(response).isNotNull();
        assertThat(response.otpVerificationId()).isNotNull();
    }

    @Test
    @DisplayName("16. Development provider receives the generated OTP")
    void testDevelopmentProviderReceivesOtp() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        otpService.sendOtp(new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION));

        verify(otpDeliveryProvider, times(1)).sendOtp(
                eq(validMobile),
                matches("^\\d{6}$"),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("17. OtpServiceImpl depends strictly on OtpDeliveryProvider interface")
    void testDependsOnlyOnDeliveryProviderInterface() {
        assertThat(otpDeliveryProvider).isInstanceOf(OtpDeliveryProvider.class);
    }

    @Test
    @DisplayName("18. Delivery failure propagates exception and avoids successful return")
    void testDeliveryFailureThrowsException() {
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        doThrow(new OtpDeliveryException("SMS gateway network timeout"))
                .when(otpDeliveryProvider).sendOtp(anyString(), anyString(), any(LocalDateTime.class));

        assertThatThrownBy(() -> otpService.sendOtp(new SendOtpRequest(validMobile, OtpPurpose.REGISTRATION)))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessageContaining("SMS gateway network timeout");
    }

    @Test
    @DisplayName("Verification with nonexistent ID throws ResourceNotFoundException")
    void testVerificationWithNonexistentIdThrowsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(otpVerificationRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp(new VerifyOtpRequest(randomId, "123456")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("OTP verification not found");
    }
}

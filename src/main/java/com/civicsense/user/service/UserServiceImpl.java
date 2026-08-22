package com.civicsense.user.service;

import com.civicsense.otp.entity.OtpStatus;
import com.civicsense.otp.entity.OtpVerification;
import com.civicsense.otp.repository.OtpVerificationRepository;
import com.civicsense.role.entity.Role;
import com.civicsense.role.repository.RoleRepository;
import com.civicsense.user.dto.CreateUserRequest;
import com.civicsense.user.dto.CreateUserResponse;
import com.civicsense.user.entity.User;
import com.civicsense.user.entity.UserStatus;
import com.civicsense.user.repository.UserRepository;
import com.civicsense.verification.entity.IdentityVerification;
import com.civicsense.verification.entity.VerificationStatus;
import com.civicsense.verification.repository.IdentityVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String CITIZEN_ROLE = "CITIZEN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    @Transactional
    public CreateUserResponse createUser(
            CreateUserRequest request
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

        // 2. OTP must be successfully verified
        if (otpVerification.getStatus() != OtpStatus.VERIFIED) {
            throw new IllegalStateException(
                    "OTP must be verified before creating user"
            );
        }

        // 3. Get the identity verification linked to this OTP
        IdentityVerification identityVerification =
                otpVerification.getIdentityVerification();

        if (identityVerification == null) {
            throw new IllegalStateException(
                    "Identity verification not linked to OTP"
            );
        }

        // 4. Identity details must have been confirmed by the user
        if (identityVerification.getVerificationStatus()
                != VerificationStatus.USER_CONFIRMED) {

            throw new IllegalStateException(
                    "Identity details have not been confirmed"
            );
        }

        // 5. Get the verified mobile number
        String mobileNumber =
                otpVerification.getMobileNumber();

        // 6. Prevent duplicate user registration
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new IllegalStateException(
                    "Mobile number is already registered"
            );
        }

        // 7. Get the default citizen role
        Role citizenRole =
                roleRepository.findByName(CITIZEN_ROLE)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "CITIZEN role not found"
                                )
                        );

        // 8. Create user
        User user = new User();

        // Use user-confirmed identity data
        user.setName(
                identityVerification.getConfirmedName()
        );

        // Use mobile number whose OTP was verified
        user.setMobileNumber(mobileNumber);

        // Hash password before storing
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setStatus(UserStatus.ACTIVE);
        user.setMobileVerified(true);

        /*
         * Current registration flow has user-confirmed
         * identity data, but not external/government
         * identity verification.
         */
        user.setIdentityVerified(true);

        // Assign default citizen role
        user.getRoles().add(citizenRole);

        // 9. Save user
        User savedUser =
                userRepository.save(user);

        // 10. Link identity verification to the new user
        identityVerification.setUser(savedUser);

        identityVerificationRepository.save(
                identityVerification
        );

        // 11. Link OTP verification to the new user
        otpVerification.setUser(savedUser);

        otpVerificationRepository.save(
                otpVerification
        );

        // 12. Return registration result
        return new CreateUserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getMobileNumber(),
                CITIZEN_ROLE,
                savedUser.getStatus().name(),
                "User registered successfully"
        );
    }
}
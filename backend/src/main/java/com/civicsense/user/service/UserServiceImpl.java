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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {

        // 1. Find the OTP verification record
        OtpVerification otpVerification =
                otpVerificationRepository.findById(request.otpVerificationId())
                        .orElseThrow(() -> new RuntimeException("OTP verification not found"));

        // 2. OTP must be successfully verified
        if (otpVerification.getStatus() != OtpStatus.VERIFIED) {
            throw new IllegalStateException("OTP must be verified before creating user");
        }

        // 3. Get the verified mobile number from the OTP record
        String mobileNumber = otpVerification.getMobileNumber();

        // 4. Prevent duplicate user registration by mobile
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new IllegalStateException("Mobile number is already registered");
        }

        // 5. Prevent duplicate registration by email (if provided)
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email address is already registered");
        }

        // 6. Get the default citizen role
        Role citizenRole = roleRepository.findByName(CITIZEN_ROLE)
                .orElseThrow(() -> new RuntimeException("CITIZEN role not found"));

        // 7. Build the new user
        User user = new User();

        // Name comes from the registration form (passed in CreateUserRequest)
        user.setName(request.name() != null ? request.name() : "Citizen");

        // Mobile from the OTP-verified record — guaranteed authentic
        user.setMobileNumber(mobileNumber);

        // Email stored but no OTP sent for it
        user.setEmail(request.email());

        // Hash password before storing — never store plaintext
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        user.setStatus(UserStatus.ACTIVE);
        user.setMobileVerified(true);

        /*
         * Identity (Aadhaar OCR) is no longer part of the registration flow.
         * identityVerified is false until a future ID-verification step is added.
         */
        user.setIdentityVerified(false);

        // Assign default citizen role
        user.getRoles().add(citizenRole);

        // 8. Save user
        User savedUser = userRepository.save(user);

        // 9. Link the OTP verification record to the new user
        otpVerification.setUser(savedUser);
        otpVerificationRepository.save(otpVerification);

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
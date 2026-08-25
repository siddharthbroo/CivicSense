package com.civicsense.auth.service;

import com.civicsense.auth.dto.LoginRequest;
import com.civicsense.auth.dto.LoginResponse;
import com.civicsense.exception.InvalidCredentialsException;
import com.civicsense.user.entity.User;
import com.civicsense.user.entity.UserStatus;
import com.civicsense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {

        // 1. Find user by mobile number
        User user = userRepository
                .findByMobileNumber(request.mobileNumber())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid mobile number or password"
                        )
                );

        // 2. Check account status
        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new InvalidCredentialsException(
                    "User account is not active"
            );
        }

        // 3. Verify password
        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {

            throw new InvalidCredentialsException(
                    "Invalid mobile number or password"
            );
        }

        // 4. Get user's role
        String role = user.getRoles()
                .stream()
                .findFirst()
                .map(roleEntity -> roleEntity.getName())
                .orElse("CITIZEN");

        // 5. Generate JWT
        String token = jwtService.generateToken(user);

        // 6. Return login response
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getMobileNumber(),
                role,
                user.getStatus().name(),
                token,
                "Login successful"
        );
    }
}
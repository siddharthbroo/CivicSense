package com.civicsense.user.service;

import com.civicsense.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String testMobile = "9876543210";
    private final String testEmail = "citizen@example.com";
    private final String testName = "Jane Doe";
    private final String testPassword = "Password@123";

    private UUID otpVerificationId;
    private OtpVerification verifiedOtp;
    private Role citizenRole;

    @BeforeEach
    void setUp() {
        otpVerificationId = UUID.randomUUID();
        verifiedOtp = new OtpVerification();
        verifiedOtp.setId(otpVerificationId);
        verifiedOtp.setMobileNumber(testMobile);
        verifiedOtp.setStatus(OtpStatus.VERIFIED);

        citizenRole = new Role();
        citizenRole.setName("CITIZEN");
    }

    @Test
    @DisplayName("Registers user successfully without IdentityVerification dependency")
    void testRegisterUserSuccess() {
        when(otpVerificationRepository.findById(otpVerificationId)).thenReturn(Optional.of(verifiedOtp));
        when(userRepository.existsByMobileNumber(testMobile)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(roleRepository.findByName("CITIZEN")).thenReturn(Optional.of(citizenRole));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        CreateUserRequest request = new CreateUserRequest(otpVerificationId, testName, testEmail, testPassword);
        CreateUserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(testName);
        assertThat(response.mobileNumber()).isEqualTo(testMobile);
        assertThat(response.role()).isEqualTo("CITIZEN");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE.name());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo(testName);
        assertThat(savedUser.getMobileNumber()).isEqualTo(testMobile);
        assertThat(savedUser.getEmail()).isEqualTo(testEmail);
        assertThat(savedUser.isMobileVerified()).isTrue();
        assertThat(savedUser.isIdentityVerified()).isFalse();
        assertThat(passwordEncoder.matches(testPassword, savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly("CITIZEN").doesNotContain("OFFICER");

        verify(otpVerificationRepository).save(verifiedOtp);
        assertThat(verifiedOtp.getUser()).isEqualTo(savedUser);
    }

    @Test
    @DisplayName("Fails registration if OTP is not yet verified")
    void testRegisterUserFailsWhenOtpNotVerified() {
        verifiedOtp.setStatus(OtpStatus.PENDING);
        when(otpVerificationRepository.findById(otpVerificationId)).thenReturn(Optional.of(verifiedOtp));

        CreateUserRequest request = new CreateUserRequest(otpVerificationId, testName, testEmail, testPassword);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OTP must be verified before creating user");
    }

    @Test
    @DisplayName("Fails registration if mobile number is already registered")
    void testRegisterUserFailsDuplicateMobile() {
        when(otpVerificationRepository.findById(otpVerificationId)).thenReturn(Optional.of(verifiedOtp));
        when(userRepository.existsByMobileNumber(testMobile)).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest(otpVerificationId, testName, testEmail, testPassword);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mobile number is already registered");
    }

    @Test
    @DisplayName("Fails registration if email is already registered")
    void testRegisterUserFailsDuplicateEmail() {
        when(otpVerificationRepository.findById(otpVerificationId)).thenReturn(Optional.of(verifiedOtp));
        when(userRepository.existsByMobileNumber(testMobile)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest(otpVerificationId, testName, testEmail, testPassword);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email address is already registered");
    }
}

package com.civicsense.otp.controller;

import com.civicsense.exception.GlobalExceptionHandler;
import com.civicsense.otp.dto.SendOtpRequest;
import com.civicsense.otp.dto.SendOtpResponse;
import com.civicsense.otp.dto.VerifyOtpRequest;
import com.civicsense.otp.dto.VerifyOtpResponse;
import com.civicsense.otp.entity.OtpPurpose;
import com.civicsense.otp.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OtpControllerTest {

    @Mock
    private OtpService otpService;

    @InjectMocks
    private OtpController otpController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(otpController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/otp/send - Success with valid Indian mobile number")
    void testSendOtpSuccess() throws Exception {
        UUID otpVerificationId = UUID.randomUUID();
        when(otpService.sendOtp(any(SendOtpRequest.class)))
                .thenReturn(new SendOtpResponse(otpVerificationId, LocalDateTime.now().plusMinutes(5), "OTP generated successfully"));

        SendOtpRequest request = new SendOtpRequest("9876543210", OtpPurpose.REGISTRATION);

        mockMvc.perform(post("/api/v1/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpVerificationId").value(otpVerificationId.toString()))
                .andExpect(jsonPath("$.message").value("OTP generated successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/otp/send - Validation failure with invalid mobile number")
    void testSendOtpInvalidMobile() throws Exception {
        SendOtpRequest request = new SendOtpRequest("12345", OtpPurpose.REGISTRATION);

        mockMvc.perform(post("/api/v1/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/otp/verify - Success with valid 6-digit OTP")
    void testVerifyOtpSuccess() throws Exception {
        UUID otpVerificationId = UUID.randomUUID();
        when(otpService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenReturn(new VerifyOtpResponse(true, "OTP verified successfully"));

        VerifyOtpRequest request = new VerifyOtpRequest(otpVerificationId, "123456");

        mockMvc.perform(post("/api/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/otp/verify - Validation failure with non-6-digit OTP")
    void testVerifyOtpInvalidCodeFormat() throws Exception {
        UUID otpVerificationId = UUID.randomUUID();
        VerifyOtpRequest request = new VerifyOtpRequest(otpVerificationId, "123");

        mockMvc.perform(post("/api/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}

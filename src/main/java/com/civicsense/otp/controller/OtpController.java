package com.civicsense.otp.controller;

import com.civicsense.otp.dto.SendOtpRequest;
import com.civicsense.otp.dto.SendOtpResponse;
import com.civicsense.otp.dto.VerifyOtpRequest;
import com.civicsense.otp.dto.VerifyOtpResponse;
import com.civicsense.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<SendOtpResponse> sendOtp(
            @RequestBody SendOtpRequest request
    ) {

        SendOtpResponse response =
                otpService.sendOtp(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request
    ) {

        VerifyOtpResponse response =
                otpService.verifyOtp(request);

        return ResponseEntity.ok(response);
    }
}
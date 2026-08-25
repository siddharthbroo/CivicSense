package com.civicsense.otp.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptOtpHasher implements OtpHasher {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    public String hash(String otp) {
        return passwordEncoder.encode(otp);
    }

    @Override
    public boolean matches(String otp, String hash) {
        return passwordEncoder.matches(otp, hash);
    }
}
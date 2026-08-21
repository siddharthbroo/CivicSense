package com.civicsense.otp.service;

public interface OtpHasher {

    String hash(String otp);

    boolean matches(String otp, String hash);
}
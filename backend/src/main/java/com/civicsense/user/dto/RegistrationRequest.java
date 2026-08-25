package com.civicsense.user.dto;

public record RegistrationRequest(
        String name,
        String mobileNumber,
        String email,
        String password
) {
}
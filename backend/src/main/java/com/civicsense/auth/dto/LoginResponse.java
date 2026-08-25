package com.civicsense.auth.dto;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String name,
        String mobileNumber,
        String role,
        String status,
        String token,
        String message
) {
}
package com.civicsense.user.dto;

import java.util.UUID;

public record CreateUserResponse(
        UUID userId,
        String name,
        String mobileNumber,
        String role,
        String status,
        String message
) {
}
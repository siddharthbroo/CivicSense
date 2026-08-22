package com.civicsense.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        @NotBlank(message = "Password is required")
        String password
) {
}
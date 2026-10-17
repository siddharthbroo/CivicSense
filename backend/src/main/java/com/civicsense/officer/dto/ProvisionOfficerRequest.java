package com.civicsense.officer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProvisionOfficerRequest(
        @NotNull(message = "User ID is required to link an officer profile")
        UUID userId,

        @NotBlank(message = "Employee code is required")
        @Size(max = 50, message = "Employee code must not exceed 50 characters")
        String employeeCode,

        @Size(max = 150, message = "Municipality name must not exceed 150 characters")
        String municipality,

        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @NotBlank(message = "Department is required")
        @Size(max = 100, message = "Department must not exceed 100 characters")
        String department,

        @Email(message = "Government email must be a valid email address")
        @Size(max = 150, message = "Government email must not exceed 150 characters")
        String governmentEmail
) {}


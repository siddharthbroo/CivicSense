package com.civicsense.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(

        @NotBlank(message = "Complaint description is required")
        @Size(
                min = 10,
                max = 5000,
                message = "Complaint description must be between 10 and 5000 characters"
        )
        String description,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        @Size(
                max = 500,
                message = "Address must not exceed 500 characters"
        )
        String address
) {
}
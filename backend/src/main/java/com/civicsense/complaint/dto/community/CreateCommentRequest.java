package com.civicsense.complaint.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content cannot be blank")
        @Size(max = 1000, message = "Comment content must not exceed 1000 characters")
        String content
) {
}


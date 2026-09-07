package com.civicsense.complaint.service.ai;

public record AiDuplicateVerificationResult(
        String verdict,
        double confidence,
        String reasoning
) {
}
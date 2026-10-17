package com.civicsense.complaint.service.verification;

/**
 * Result of the deterministic Verification Gate evaluation.
 *
 * @param verificationRequired Whether field/officer verification is required before automated resolution.
 * @param reason               Explainable reason for the decision (e.g. HIGH_SEVERITY_LOW_EVIDENCE, SUSPICIOUS_FLAGS, NORMAL_FLOW).
 */
public record VerificationDecision(
        boolean verificationRequired,
        String reason
) {
    public static VerificationDecision required(String reason) {
        return new VerificationDecision(true, reason);
    }

    public static VerificationDecision notRequired(String reason) {
        return new VerificationDecision(false, reason);
    }
}
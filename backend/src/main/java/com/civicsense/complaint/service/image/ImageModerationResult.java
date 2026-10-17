package com.civicsense.complaint.service.image;

/**
 * Result of image validation, safety moderation, and complaint relevance evaluation.
 *
 * @param valid whether the file is technically valid (type, size, decodable)
 * @param prohibited whether the image is classified with high confidence as prohibited (safety violation)
 * @param unrelated whether the image is clearly unrelated to the civic complaint category/description
 * @param quarantined whether the image is borderline/uncertain or moderation/relevance was unavailable
 * @param reason human-readable reason for prohibition, irrelevance, failure, or advisory
 * @param category category of prohibited or classification issue
 * @param relevanceStatus relevance classification: RELEVANT, POTENTIALLY_RELEVANT, UNRELATED, UNCERTAIN
 * @param confidence confidence level between 0.0 and 1.0
 */
public record ImageModerationResult(
        boolean valid,
        boolean prohibited,
        boolean unrelated,
        boolean quarantined,
        String reason,
        String category,
        String relevanceStatus,
        double confidence
) {
    public static ImageModerationResult approved(String message, String relevanceStatus) {
        return new ImageModerationResult(true, false, false, false, message, "SAFE", relevanceStatus, 1.0);
    }

    public static ImageModerationResult prohibited(String reason, String category, double confidence) {
        return new ImageModerationResult(false, true, false, false, reason, category, "PROHIBITED", confidence);
    }

    public static ImageModerationResult unrelated(String reason, double confidence) {
        return new ImageModerationResult(false, false, true, false, reason, "UNRELATED_CONTENT", "UNRELATED", confidence);
    }

    public static ImageModerationResult quarantined(String reason, String category, String relevanceStatus, double confidence) {
        return new ImageModerationResult(true, false, false, true, reason, category, relevanceStatus, confidence);
    }

    public static ImageModerationResult invalidFile(String reason) {
        return new ImageModerationResult(false, false, false, false, reason, "INVALID_FILE", "INVALID", 1.0);
    }
}

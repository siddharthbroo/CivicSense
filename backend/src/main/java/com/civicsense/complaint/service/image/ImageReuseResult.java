package com.civicsense.complaint.service.image;

import com.civicsense.complaint.entity.image.ImageReuseType;

/**
 * Result of evaluating an image for cross-complaint reuse and provenance.
 *
 * @param reused whether identical or visually equivalent images were found in prior complaints
 * @param reuseCount total number of prior matching complaints found
 * @param reuseType contextual classification of reuse
 * @param suspicious whether this reuse constitutes a suspicious finding (e.g. cross-location)
 * @param summary human-readable explanation of the finding
 * @param imageHash SHA-256 hash of the image bytes
 * @param perceptualHash 64-bit dHash perceptual hash
 */
public record ImageReuseResult(
        boolean reused,
        int reuseCount,
        ImageReuseType reuseType,
        boolean suspicious,
        String summary,
        String imageHash,
        String perceptualHash
) {
    public static ImageReuseResult none(String sha256, String dHash) {
        return new ImageReuseResult(
                false,
                0,
                ImageReuseType.NONE,
                false,
                "Unique image — no prior reuse detected.",
                sha256,
                dHash
        );
    }

    public static ImageReuseResult resubmission(int count, String sha256, String dHash) {
        return new ImageReuseResult(
                true,
                count,
                ImageReuseType.RESUBMISSION,
                false,
                "Likely resubmission or follow-up by the same citizen at the same location.",
                sha256,
                dHash
        );
    }

    public static ImageReuseResult communityShared(int count, String sha256, String dHash) {
        return new ImageReuseResult(
                true,
                count,
                ImageReuseType.COMMUNITY_SHARED,
                false,
                "Shared civic evidence: reported at the same location by another citizen.",
                sha256,
                dHash
        );
    }

    public static ImageReuseResult differentLocation(int count, boolean sameUser, String sha256, String dHash) {
        String userContext = sameUser ? "by the same user" : "across different users";
        return new ImageReuseResult(
                true,
                count,
                ImageReuseType.DIFFERENT_LOCATION,
                true,
                "Suspicious image reuse: identical image previously submitted " + userContext + " at a different location.",
                sha256,
                dHash
        );
    }

    public static ImageReuseResult multiLocationDispersed(int count, int distinctLocations, String sha256, String dHash) {
        return new ImageReuseResult(
                true,
                count,
                ImageReuseType.MULTI_LOCATION_DISPERSED,
                true,
                "Strong suspicious signal: identical image reused across " + distinctLocations + " distinct locations.",
                sha256,
                dHash
        );
    }
}


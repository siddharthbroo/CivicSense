package com.civicsense.complaint.entity.image;

/**
 * Categorization of image reuse detected across civic complaints.
 */
public enum ImageReuseType {
    /**
     * Unique image — no previous occurrences found in the system.
     */
    NONE,

    /**
     * Same user, same location (<= 150m): likely a resubmission or follow-up by the same citizen.
     * Not treated as fraud.
     */
    RESUBMISSION,

    /**
     * Different users, same location (<= 150m): multiple citizens reporting the same civic issue.
     * Legitimate shared community evidence; not treated as fraud.
     */
    COMMUNITY_SHARED,

    /**
     * Same or different user, but reused at a different geographic location (> 150m).
     * Suspicious signal for provenance review.
     */
    DIFFERENT_LOCATION,

    /**
     * Same image reused across 3 or more distinct geographic locations.
     * Stronger suspicious signal indicating dispersed stock/reused imagery.
     */
    MULTI_LOCATION_DISPERSED
}


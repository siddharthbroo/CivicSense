package com.civicsense.complaint.entity.geo;

/**
 * Indicates the provenance and verification status of an administrative ward's geographic boundary.
 *
 * <p>Prevents disguising provisional or approximate bounding boxes as verified municipal delimitations.
 */
public enum BoundaryProvenance {
    /**
     * Officially surveyed and verified GIS delimitation boundary from municipal authorities.
     */
    VERIFIED_BOUNDARY,

    /**
     * Approximate or provisional bounding box configured for intake resolution and testing.
     * Not an officially verified GIS delimitation boundary.
     */
    PROVISIONAL_BOUNDS,

    /**
     * Official administrative ward record with verified name and identifier from official orders,
     * but without spatial boundary data currently ingested.
     */
    NO_BOUNDARY
}


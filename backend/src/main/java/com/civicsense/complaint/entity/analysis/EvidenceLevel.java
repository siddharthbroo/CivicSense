package com.civicsense.complaint.entity.analysis;

/**
 * Evidence Level for a complaint.
 *
 * <ul>
 *   <li>E0 - Text only (no photo submitted).</li>
 *   <li>E1 - Photo submitted; description-image consistency confirmed.</li>
 *   <li>E2 - Photo + meaningful location/time corroboration
 *            (EXIF GPS close to confirmed pin, or EXIF timestamp fresh).</li>
 *   <li>E3 - Strong independent/field verification (reserved for future use).</li>
 * </ul>
 *
 * Evidence level is computed deterministically after Gemini M3 analysis.
 * It is stored for use by a future verification gate in M5.
 * It does NOT currently affect the risk-score formula.
 */
public enum EvidenceLevel {

    /** No photo submitted. Text description only. */
    E0,

    /** Photo present and consistent with description. */
    E1,

    /** Photo present with location/time corroboration (EXIF near pin, fresh timestamp). */
    E2,

    /** Strong independent or field verification (reserved). */
    E3
}
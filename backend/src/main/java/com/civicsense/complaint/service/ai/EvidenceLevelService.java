package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.EvidenceLevel;
import com.civicsense.complaint.entity.image.ComplaintImage;
import org.springframework.stereotype.Service;

/**
 * Deterministically computes the Evidence Level (E0–E3) for a complaint.
 *
 * <p>Rules:
 * <ul>
 *   <li><b>E0</b> – No photo submitted (text-only complaint).</li>
 *   <li><b>E1</b> – Photo present AND description-image consistency is
 *       CONSISTENT or UNCERTAIN (not INCONSISTENT).</li>
 *   <li><b>E2</b> – Photo present AND description-image consistency is
 *       CONSISTENT AND meaningful location/time corroboration exists
 *       (EXIF GPS is present and within 500 m of the confirmed pin,
 *        OR EXIF timestamp is present).</li>
 *   <li><b>E3</b> – Reserved for future independent/field verification (not assigned here).</li>
 * </ul>
 *
 * <p>The evidence level does NOT affect the M5 risk-score formula.
 * It is stored for use by a future verification gate.
 */
@Service
public class EvidenceLevelService {

    /**
     * Compute evidence level.
     *
     * @param complaintImage   The saved ComplaintImage entity, or {@code null} if no photo.
     * @param descriptionImageConsistency The Gemini-returned consistency value (may be null).
     * @return The computed EvidenceLevel.
     */
    public EvidenceLevel compute(
            ComplaintImage complaintImage,
            DescriptionImageConsistency descriptionImageConsistency) {

        // E0: no photo at all
        if (complaintImage == null
                || complaintImage.getImageUrl() == null
                || complaintImage.getImageUrl().isBlank()) {
            return EvidenceLevel.E0;
        }

        // Photo present — at minimum E1
        // Avoid promoting to E1 when Gemini flagged the image as clearly inconsistent
        boolean consistencyOk =
                descriptionImageConsistency == null
                        || descriptionImageConsistency != DescriptionImageConsistency.INCONSISTENT;

        if (!consistencyOk) {
            // Photo present but description-image is inconsistent — still E0 equivalent
            // (we do NOT raise E1 for clearly inconsistent images)
            return EvidenceLevel.E0;
        }

        // Check for meaningful location/time corroboration → E2
        boolean hasExifCorroboration = hasLocationOrTimeCorroboration(complaintImage);

        if (descriptionImageConsistency == DescriptionImageConsistency.CONSISTENT
                && hasExifCorroboration) {
            return EvidenceLevel.E2;
        }

        // Photo present, consistent (or uncertain), no strong corroboration → E1
        return EvidenceLevel.E1;
    }

    /**
     * Returns true if the image has EXIF GPS within acceptable distance of the
     * confirmed complaint pin, OR if EXIF timestamp is present.
     * The distance threshold matches the EXIF mismatch flag threshold (500 m).
     */
    private boolean hasLocationOrTimeCorroboration(ComplaintImage image) {
        // EXIF GPS present and NOT flagged as a mismatch (i.e. within 500 m)
        boolean exifGpsCorroborates =
                Boolean.TRUE.equals(image.getHasExifGps())
                        && !Boolean.TRUE.equals(image.getExifLocationMismatch());

        // EXIF timestamp available
        boolean exifTimestampPresent = image.getExifTimestamp() != null;

        // Suspicious cross-location image reuse should not grant verified E2 corroboration
        boolean noSuspiciousReuse = !Boolean.TRUE.equals(image.getImageReuseSuspicious());

        return (exifGpsCorroborates || exifTimestampPresent) && noSuspiciousReuse;
    }
}
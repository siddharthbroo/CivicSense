package com.civicsense.complaint.service.complaintCluster;

import org.springframework.stereotype.Service;

/**
 * Calculates the combined duplicate score for two complaint candidates.
 *
 * <p>Weight re-normalization rules:
 * <ul>
 *   <li>When both complaints have images: all 6 weights are used as declared.</li>
 *   <li>When one or both complaints lack an image (imageSimilarity == 0.0 and
 *       {@code hasImageComparison == false}): IMAGE_WEIGHT is redistributed
 *       proportionally across the remaining 5 signals so the total remains 1.0.</li>
 *   <li>Address comparison is optional as before — when address strings are absent,
 *       ADDRESS_WEIGHT is redistributed similarly.</li>
 * </ul>
 */
@Service
public class ComplaintDuplicateScoringService {

    private static final double TITLE_WEIGHT = 0.10;
    private static final double DESCRIPTION_WEIGHT = 0.15;
    private static final double SUMMARY_WEIGHT = 0.15;
    private static final double IMAGE_WEIGHT = 0.25;
    private static final double ADDRESS_WEIGHT = 0.15;
    private static final double LOCATION_WEIGHT = 0.20;

    private static final double LOCATION_MAX_DISTANCE_METERS = 50.0;

    private static final double DUPLICATE_THRESHOLD = 0.78;
    private static final double POTENTIAL_DUPLICATE_THRESHOLD = 0.60;

    /**
     * Convenience overload — assumes both address and image comparisons are available.
     */
    public double calculateCombinedScore(
            double titleSimilarity,
            double descriptionSimilarity,
            double summarySimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters) {
        return calculateCombinedScore(
                titleSimilarity,
                descriptionSimilarity,
                summarySimilarity,
                imageSimilarity,
                addressSimilarity,
                distanceMeters,
                true,   // hasAddressComparison
                true    // hasImageComparison
        );
    }

    /**
     * Overload for callers that know whether address comparison is valid but
     * assume image comparison is always available (legacy call-site compatibility).
     */
    public double calculateCombinedScore(
            double titleSimilarity,
            double descriptionSimilarity,
            double summarySimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters,
            boolean hasAddressComparison) {
        return calculateCombinedScore(
                titleSimilarity,
                descriptionSimilarity,
                summarySimilarity,
                imageSimilarity,
                addressSimilarity,
                distanceMeters,
                hasAddressComparison,
                true    // hasImageComparison — legacy default
        );
    }

    /**
     * Full signature with explicit image-comparison flag.
     *
     * @param hasAddressComparison {@code true} when both complaints have address strings.
     * @param hasImageComparison   {@code true} when BOTH complaints have an uploaded image.
     */
    public double calculateCombinedScore(
            double titleSimilarity,
            double descriptionSimilarity,
            double summarySimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters,
            boolean hasAddressComparison,
            boolean hasImageComparison) {

        double locationSimilarity = calculateLocationSimilarity(distanceMeters);

        double raw =
                (titleSimilarity * TITLE_WEIGHT)
                        + (descriptionSimilarity * DESCRIPTION_WEIGHT)
                        + (summarySimilarity * SUMMARY_WEIGHT)
                        + (imageSimilarity * IMAGE_WEIGHT)
                        + (addressSimilarity * ADDRESS_WEIGHT)
                        + (locationSimilarity * LOCATION_WEIGHT);

        // Determine the total weight actually used so we can normalise.
        double totalWeight = 1.0;

        if (!hasImageComparison) {
            // Image weight was not used — subtract it from the denominator.
            totalWeight -= IMAGE_WEIGHT;
        }

        if (!hasAddressComparison) {
            // Address weight was not used — subtract it from the denominator.
            totalWeight -= ADDRESS_WEIGHT;
        }

        // Normalise: divide raw by the fraction of weight actually used.
        // Guard against totalWeight being <= 0 (should never happen in practice).
        if (totalWeight <= 0.0) {
            return 0.0;
        }

        return raw / totalWeight;
    }

    public DuplicateDecision decide(
            double combinedScore,
            boolean categoryMatch) {

        if (!categoryMatch) {
            return DuplicateDecision.SEPARATE;
        }

        if (combinedScore >= DUPLICATE_THRESHOLD) {
            return DuplicateDecision.DUPLICATE;
        }

        if (combinedScore >= POTENTIAL_DUPLICATE_THRESHOLD) {
            return DuplicateDecision.POTENTIAL_DUPLICATE;
        }

        return DuplicateDecision.SEPARATE;
    }

    private double calculateLocationSimilarity(double distanceMeters) {

        if (distanceMeters <= 25.0) {
            return 1.0;
        }

        if (distanceMeters >= LOCATION_MAX_DISTANCE_METERS) {
            return 0.0;
        }

        return 1.0 -
                ((distanceMeters - 25.0) /
                        (LOCATION_MAX_DISTANCE_METERS - 25.0));
    }

    public enum DuplicateDecision {
        DUPLICATE,
        POTENTIAL_DUPLICATE,
        SEPARATE
    }
}
package com.civicsense.complaint.service;

import org.springframework.stereotype.Service;

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

    public double calculateCombinedScore(
            double titleSimilarity,
            double descriptionSimilarity,
            double summarySimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters
    ) {

        double locationSimilarity =
                calculateLocationSimilarity(distanceMeters);

        return
                (titleSimilarity * TITLE_WEIGHT)
                        + (descriptionSimilarity * DESCRIPTION_WEIGHT)
                        + (summarySimilarity * SUMMARY_WEIGHT)
                        + (imageSimilarity * IMAGE_WEIGHT)
                        + (addressSimilarity * ADDRESS_WEIGHT)
                        + (locationSimilarity * LOCATION_WEIGHT);
    }

    public DuplicateDecision decide(
            double combinedScore,
            boolean categoryMatch
    ) {

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
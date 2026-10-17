package com.civicsense.complaint.service.geo;

/**
 * Result of validating an incident location against CivicSense coordinate rules,
 * supported service area (Delhi NCR), and geographic plausibility.
 */
public record LocationValidationResult(
        boolean valid,
        boolean withinServiceArea,
        boolean swappedSuggested,
        Double suggestedLatitude,
        Double suggestedLongitude,
        boolean waterBodyWarning,
        String waterBodyName,
        String errorMessage,
        String advisoryMessage
) {
    public static LocationValidationResult success(boolean waterBodyWarning, String waterBodyName, String advisoryMessage) {
        return new LocationValidationResult(
                true,
                true,
                false,
                null,
                null,
                waterBodyWarning,
                waterBodyName,
                null,
                advisoryMessage
        );
    }

    public static LocationValidationResult invalid(String errorMessage) {
        return new LocationValidationResult(
                false,
                false,
                false,
                null,
                null,
                false,
                null,
                errorMessage,
                null
        );
    }

    public static LocationValidationResult swapped(double suggestedLat, double suggestedLon, String errorMessage) {
        return new LocationValidationResult(
                false,
                false,
                true,
                suggestedLat,
                suggestedLon,
                false,
                null,
                errorMessage,
                null
        );
    }

    public static LocationValidationResult outsideServiceArea(String errorMessage) {
        return new LocationValidationResult(
                false,
                false,
                false,
                null,
                null,
                false,
                null,
                errorMessage,
                null
        );
    }
}


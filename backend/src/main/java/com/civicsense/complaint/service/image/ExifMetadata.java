package com.civicsense.complaint.service.image;

import java.time.LocalDateTime;

/**
 * Encapsulates GPS and timestamp metadata extracted server-side from image EXIF headers.
 *
 * All fields are optional/nullable.
 */
public record ExifMetadata(
        Double latitude,
        Double longitude,
        LocalDateTime timestamp,
        boolean hasGps,
        Double distanceMetersToPin,
        boolean locationMismatchFlag
) {
    public static ExifMetadata empty() {
        return new ExifMetadata(null, null, null, false, null, false);
    }
}
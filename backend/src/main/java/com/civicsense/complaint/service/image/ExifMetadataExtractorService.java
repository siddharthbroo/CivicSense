package com.civicsense.complaint.service.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class ExifMetadataExtractorService {

    // If EXIF GPS location is further than 500m from confirmed pin, flag as mismatch
    private static final double MISMATCH_THRESHOLD_METERS = 500.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    public ExifMetadata extractMetadata(MultipartFile file, Double confirmedLat, Double confirmedLon) {
        if (file == null || file.isEmpty()) {
            return ExifMetadata.empty();
        }

        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            Double exifLat = null;
            Double exifLon = null;
            boolean hasGps = false;

            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    exifLat = geoLocation.getLatitude();
                    exifLon = geoLocation.getLongitude();
                    hasGps = true;
                }
            }

            LocalDateTime exifTimestamp = null;
            ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDDirectory != null) {
                Date dateOriginal = subIFDDirectory.getDateOriginal();
                if (dateOriginal != null) {
                    exifTimestamp = dateOriginal.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }

            Double distanceMeters = null;
            boolean mismatchFlag = false;

            if (hasGps && confirmedLat != null && confirmedLon != null) {
                distanceMeters = calculateDistanceMeters(exifLat, exifLon, confirmedLat, confirmedLon);
                if (distanceMeters > MISMATCH_THRESHOLD_METERS) {
                    mismatchFlag = true;
                }
            }

            return new ExifMetadata(
                    exifLat,
                    exifLon,
                    exifTimestamp,
                    hasGps,
                    distanceMeters,
                    mismatchFlag
            );

        } catch (Exception e) {
            // Missing EXIF or unparseable metadata is normal and should never cause rejection
            System.out.println("EXIF extraction skipped or unavailable: " + e.getMessage());
            return ExifMetadata.empty();
        }
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Safely strips all EXIF/metadata from the image bytes by re-encoding via ImageIO.
     * Prevents leaking GPS and device metadata through public cloud image storage.
     * If re-encoding fails, falls back to original bytes.
     */
    public byte[] stripExifMetadata(byte[] originalBytes, String contentType) {
        if (originalBytes == null || originalBytes.length == 0) {
            return originalBytes;
        }

        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(originalBytes);
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {

            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bais);
            if (img == null) {
                return originalBytes;
            }

            String formatName = "jpg";
            if (contentType != null && contentType.toLowerCase().contains("png")) {
                formatName = "png";
            }

            boolean written = javax.imageio.ImageIO.write(img, formatName, baos);
            if (written && baos.size() > 0) {
                return baos.toByteArray();
            }
        } catch (Exception e) {
            System.err.println("EXIF stripping fallback to original: " + e.getMessage());
        }

        return originalBytes;
    }
}
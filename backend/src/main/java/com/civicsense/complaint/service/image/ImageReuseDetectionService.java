package com.civicsense.complaint.service.image;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for detecting cross-complaint image reuse using SHA-256 and perceptual difference hashing.
 *
 * <p>Context rules:
 * <ul>
 *   <li>Same image, same user, same location (<= 150m): likely resubmission / follow-up; NOT fraud.</li>
 *   <li>Same image, different users, same location (<= 150m): shared civic evidence; NOT fraud.</li>
 *   <li>Same image, same or different user, different location (> 150m): suspicious provenance signal.</li>
 *   <li>Same image reused across 3+ distinct locations: stronger suspicious signal for review.</li>
 * </ul>
 *
 * <p>Image reuse findings NEVER automatically reject a complaint or reduce its priority.
 */
@Service
@RequiredArgsConstructor
public class ImageReuseDetectionService {

    private static final double SAME_LOCATION_THRESHOLD_METERS = 150.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final ComplaintImageRepository complaintImageRepository;

    public ImageReuseResult detectReuse(
            byte[] imageBytes,
            UUID currentUserId,
            Double currentLat,
            Double currentLon,
            UUID currentComplaintId
    ) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ImageReuseResult.none(null, null);
        }

        String sha256 = ImageHasher.computeSha256(imageBytes);
        String dHash = ImageHasher.computeDHash(imageBytes);

        if (sha256 == null) {
            return ImageReuseResult.none(null, null);
        }

        // 1. Primary lookup: byte-identical SHA-256 matches
        List<ComplaintImage> matches = new ArrayList<>(complaintImageRepository.findByImageHash(sha256));

        // 2. Secondary lookup: if no exact SHA-256 match, look for identical perceptual dHash
        if (matches.isEmpty() && dHash != null) {
            matches.addAll(complaintImageRepository.findByPerceptualHash(dHash));
        }

        // Exclude current complaint (avoid self-matching)
        if (currentComplaintId != null) {
            matches.removeIf(ci -> ci.getComplaint() != null && currentComplaintId.equals(ci.getComplaint().getId()));
        }

        if (matches.isEmpty()) {
            return ImageReuseResult.none(sha256, dHash);
        }

        int reuseCount = matches.size();
        boolean sameUserOnly = true;
        boolean hasDifferentLocation = false;

        // Collect locations to calculate distinct geographic locations
        List<GeoPoint> points = new ArrayList<>();
        if (currentLat != null && currentLon != null) {
            points.add(new GeoPoint(currentLat, currentLon));
        }

        for (ComplaintImage match : matches) {
            Complaint complaint = match.getComplaint();
            if (complaint == null) {
                continue;
            }

            // Check user identity
            if (currentUserId != null && complaint.getUser() != null) {
                if (!currentUserId.equals(complaint.getUser().getId())) {
                    sameUserOnly = false;
                }
            }

            // Check distance
            Double matchLat = complaint.getLatitude();
            Double matchLon = complaint.getLongitude();

            if (currentLat != null && currentLon != null && matchLat != null && matchLon != null) {
                double distance = calculateDistanceMeters(currentLat, currentLon, matchLat, matchLon);
                if (distance > SAME_LOCATION_THRESHOLD_METERS) {
                    hasDifferentLocation = true;
                }
                points.add(new GeoPoint(matchLat, matchLon));
            }
        }

        int distinctLocations = countDistinctLocations(points, SAME_LOCATION_THRESHOLD_METERS);

        // Apply contextual interpretation rules
        if (!hasDifferentLocation) {
            if (sameUserOnly) {
                return ImageReuseResult.resubmission(reuseCount, sha256, dHash);
            } else {
                return ImageReuseResult.communityShared(reuseCount, sha256, dHash);
            }
        } else {
            if (distinctLocations >= 3) {
                return ImageReuseResult.multiLocationDispersed(reuseCount, distinctLocations, sha256, dHash);
            } else {
                return ImageReuseResult.differentLocation(reuseCount, sameUserOnly, sha256, dHash);
            }
        }
    }

    private int countDistinctLocations(List<GeoPoint> points, double clusterThresholdMeters) {
        if (points.isEmpty()) {
            return 1;
        }

        List<GeoPoint> clusters = new ArrayList<>();
        for (GeoPoint p : points) {
            boolean clustered = false;
            for (GeoPoint c : clusters) {
                if (calculateDistanceMeters(p.lat, p.lon, c.lat, c.lon) <= clusterThresholdMeters) {
                    clustered = true;
                    break;
                }
            }
            if (!clustered) {
                clusters.add(p);
            }
        }
        return Math.max(1, clusters.size());
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

    private record GeoPoint(double lat, double lon) {}
}


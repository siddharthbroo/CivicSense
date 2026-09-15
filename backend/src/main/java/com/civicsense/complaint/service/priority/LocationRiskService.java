package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.risk.SensitiveLocation;
import com.civicsense.complaint.repository.SensitiveLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationRiskService {

        private static final double FULL_RISK_DISTANCE_METERS = 100.0;
        private static final double ZERO_RISK_DISTANCE_METERS = 300.0;

        private final SensitiveLocationRepository sensitiveLocationRepository;
        private final GeoapifyPlaceService geoapifyPlaceService;

        public double calculateLocationRisk(
                        Complaint complaint) {

                if (complaint.getLatitude() == null
                                || complaint.getLongitude() == null) {
                        return 0.0;
                }

                double latitude = complaint.getLatitude();
                double longitude = complaint.getLongitude();

                /*
                 * Approximate bounding box around the complaint.
                 *
                 * We use 500 meters here because the actual risk
                 * becomes zero after 300 meters.
                 *
                 * The extra margin gives us enough candidates before
                 * applying exact Haversine distance.
                 */
                double radiusMeters = 500.0;

                double latitudeDelta = radiusMeters / 111320.0;

                double longitudeDelta = radiusMeters
                                / (111320.0
                                                * Math.cos(Math.toRadians(latitude)));

                double minLatitude = latitude - latitudeDelta;

                double maxLatitude = latitude + latitudeDelta;

                double minLongitude = longitude - longitudeDelta;

                double maxLongitude = longitude + longitudeDelta;

                List<SensitiveLocation> candidates = sensitiveLocationRepository.findNearbyCandidates(
                                minLatitude,
                                maxLatitude,
                                minLongitude,
                                maxLongitude);

                if (candidates.isEmpty() && geoapifyPlaceService != null) {
                        try {
                                List<GeoapifyPlace> places = geoapifyPlaceService.findNearbyPlaces(
                                                latitude,
                                                longitude,
                                                (int) radiusMeters);

                                if (places != null && !places.isEmpty()) {
                                        geoapifyPlaceService.savePlaces(places);

                                        candidates = sensitiveLocationRepository.findNearbyCandidates(
                                                        minLatitude,
                                                        maxLatitude,
                                                        minLongitude,
                                                        maxLongitude);
                                }
                        } catch (Exception e) {
                                System.out.println("Geoapify place lookup failed: " + e.getMessage());
                        }
                }

                if (candidates.isEmpty()) {
                        return 0.0;
                }

                double highestRisk = 0.0;

                for (SensitiveLocation location : candidates) {

                        double distanceMeters = calculateDistanceMeters(
                                        latitude,
                                        longitude,
                                        location.getLatitude(),
                                        location.getLongitude());

                        double risk = calculateRiskFromDistance(
                                        distanceMeters);

                        highestRisk = Math.max(highestRisk, risk);
                }

                return highestRisk;
        }

        private double calculateRiskFromDistance(
                        double distanceMeters) {

                if (distanceMeters <= FULL_RISK_DISTANCE_METERS) {
                        return 100.0;
                }

                if (distanceMeters >= ZERO_RISK_DISTANCE_METERS) {
                        return 0.0;
                }

                return ((ZERO_RISK_DISTANCE_METERS - distanceMeters)
                                / (ZERO_RISK_DISTANCE_METERS - FULL_RISK_DISTANCE_METERS)) * 100.0;
        }

        private double calculateDistanceMeters(
                        double latitude1,
                        double longitude1,
                        double latitude2,
                        double longitude2) {

                final double earthRadiusMeters = 6371000.0;

                double latitude1Radians = Math.toRadians(latitude1);

                double latitude2Radians = Math.toRadians(latitude2);

                double deltaLatitude = Math.toRadians(latitude2 - latitude1);

                double deltaLongitude = Math.toRadians(longitude2 - longitude1);

                double a = Math.sin(deltaLatitude / 2)
                                * Math.sin(deltaLatitude / 2)
                                + Math.cos(latitude1Radians)
                                                * Math.cos(latitude2Radians)
                                                * Math.sin(deltaLongitude / 2)
                                                * Math.sin(deltaLongitude / 2);

                double c = 2 * Math.atan2(
                                Math.sqrt(a),
                                Math.sqrt(1 - a));

                return earthRadiusMeters * c;
        }
}
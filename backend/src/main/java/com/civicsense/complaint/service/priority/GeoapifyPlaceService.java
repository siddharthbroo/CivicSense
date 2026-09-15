package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.risk.SensitiveLocation;
import com.civicsense.complaint.entity.risk.SensitiveLocationType;
import com.civicsense.complaint.repository.SensitiveLocationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoapifyPlaceService {

    private final ObjectMapper objectMapper;
    private final SensitiveLocationRepository sensitiveLocationRepository;

    @Value("${geoapify.api-key}")
    private String apiKey;

    @Value("${geoapify.base-url}")
    private String baseUrl;

    private final RestClient restClient = RestClient.builder().build();

    public List<GeoapifyPlace> findNearbyPlaces(
            double latitude,
            double longitude,
            int radiusMeters
    ) {

        String url = baseUrl
                + "/v2/places"
                + "?categories=education.school,healthcare.hospital"
                + "&filter=circle:"
                + longitude + ","
                + latitude + ","
                + radiusMeters
                + "&bias=proximity:"
                + longitude + ","
                + latitude
                + "&limit=20"
                + "&apiKey="
                + apiKey;

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            List<GeoapifyPlace> places =
                    new ArrayList<>();

            JsonNode features =
                    root.get("features");

            if (features == null || !features.isArray()) {
                return places;
            }

            for (JsonNode feature : features) {

                JsonNode properties =
                        feature.get("properties");

                if (properties == null) {
                    continue;
                }

                String name =
                        properties.path("name").asText(null);

                double placeLatitude =
                        properties.path("lat").asDouble();

                double placeLongitude =
                        properties.path("lon").asDouble();

                SensitiveLocationType type =
                        determineType(properties);

                if (name == null || type == null) {
                    continue;
                }

                places.add(
                        new GeoapifyPlace(
                                name,
                                type,
                                placeLatitude,
                                placeLongitude
                        )
                );
            }

            return places;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Geoapify response",
                    e
            );
        }
    }

    public void savePlaces(
            List<GeoapifyPlace> places
    ) {

        for (GeoapifyPlace place : places) {

            boolean alreadyExists =
                    sensitiveLocationRepository
                            .findByNameAndTypeAndLatitudeAndLongitude(
                                    place.name(),
                                    place.type(),
                                    place.latitude(),
                                    place.longitude()
                            )
                            .isPresent();

            if (alreadyExists) {
                continue;
            }

            SensitiveLocation location =
                    new SensitiveLocation();

            location.setName(place.name());
            location.setType(place.type());
            location.setLatitude(place.latitude());
            location.setLongitude(place.longitude());

            sensitiveLocationRepository.save(location);
        }
    }

    private SensitiveLocationType determineType(
            JsonNode properties
    ) {

        JsonNode categories =
                properties.get("categories");

        if (categories == null || !categories.isArray()) {
            return null;
        }

        for (JsonNode category : categories) {

            String value =
                    category.asText();

            if (value.equals("education.school")) {
                return SensitiveLocationType.SCHOOL;
            }

            if (value.equals("healthcare.hospital")) {
                return SensitiveLocationType.HOSPITAL;
            }
        }

        return null;
    }
}
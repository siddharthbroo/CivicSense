package com.civicsense.complaint.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PythonAiClient {

    private final RestClient restClient;

    public PythonAiClient(
            @Value("${ai.service.url}") String aiServiceUrl
    ) {
        this.restClient = RestClient.create(aiServiceUrl);
    }

    public ComplaintTextSimilarityResponse compareComplaints(
            String title1,
            String description1,
            String summary1,
            String category1,
            String title2,
            String description2,
            String summary2,
            String category2
    ) {

        return restClient.post()
                .uri("/api/v1/embeddings/complaint-similarity")
                .body(new ComplaintTextSimilarityRequest(
                        title1,
                        description1,
                        summary1,
                        category1,
                        title2,
                        description2,
                        summary2,
                        category2
                ))
                .retrieve()
                .body(ComplaintTextSimilarityResponse.class);
    }

    public double calculateImageSimilarity(
            String imageUrl1,
            String imageUrl2
    ) {

        ImageSimilarityResponse response =
                restClient.post()
                        .uri("/api/v1/embeddings/image-similarity")
                        .body(new ImageSimilarityRequest(
                                imageUrl1,
                                imageUrl2
                        ))
                        .retrieve()
                        .body(ImageSimilarityResponse.class);

        return response.similarity();
    }


    public double calculateAddressSimilarity(
            String address1,
            String address2
    ) {

        AddressSimilarityResponse response =
                restClient.post()
                        .uri("/api/v1/similarity/address")
                        .body(new AddressSimilarityRequest(
                                address1,
                                address2
                        ))
                        .retrieve()
                        .body(AddressSimilarityResponse.class);

        return response.similarity();
    }


    public double calculateLocationDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        LocationSimilarityResponse response =
                restClient.post()
                        .uri("/api/v1/similarity/location")
                        .body(new LocationSimilarityRequest(
                                latitude1,
                                longitude1,
                                latitude2,
                                longitude2
                        ))
                        .retrieve()
                        .body(LocationSimilarityResponse.class);

        return response.distance_meters();
    }

    private record ComplaintTextSimilarityRequest(
            String title1,
            String description1,
            String summary1,
            String category1,
            String title2,
            String description2,
            String summary2,
            String category2
    ) {
    }

    private record ImageSimilarityRequest(
            String image_url1,
            String image_url2
    ) {
    }

    private record ImageSimilarityResponse(
            double similarity
    ) {
    }

    private record AddressSimilarityRequest(
            String address1,
            String address2
    ) {
    }

    private record AddressSimilarityResponse(
            double similarity
    ) {
    }

    private record LocationSimilarityRequest(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
    }

    private record LocationSimilarityResponse(
            double distance_meters
    ) {
    }
}
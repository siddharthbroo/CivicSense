package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintImage;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GeminiAiService {

    private final Client geminiClient;

    private final RestClient restClient =
            RestClient.create();

    public String analyzeComplaint(
            Complaint complaint,
            ComplaintImage complaintImage
    ) {

        // 1. Download image from ImageKit
        byte[] imageBytes = restClient
                .get()
                .uri(complaintImage.getImageUrl())
                .retrieve()
                .body(byte[].class);

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException(
                    "Unable to download complaint image"
            );
        }

        // 2. Prepare prompt
        String prompt = """
                Analyze this civic complaint using BOTH the
                complaint description and the uploaded image.

                Complaint description:
                %s

                Analyze:

                1. Detect the language.
                2. Classify the complaint category.
                3. Determine severity.
                4. Create a short summary.
                5. Recommend the responsible department.
                6. Give a confidence score between 0 and 1.
                7. Assess whether the image appears likely real,
                   suspicious, or uncertain.
                8. Check whether the image is consistent with
                   the complaint description.

                Return ONLY valid JSON.

                Required JSON fields:

                language
                category
                severity
                summary
                department
                confidence
                imageAuthenticity
                descriptionImageConsistency
                """.formatted(
                complaint.getDescription()
        );

        // 3. Create multimodal content
        Content content =
                Content.fromParts(
                        Part.fromText(prompt),
                        Part.fromBytes(
                                imageBytes,
                                complaintImage.getContentType()
                        )
                );

        // 4. Send to Gemini
        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        "gemini-2.5-flash",
                        content,
                        GenerateContentConfig.builder()
                                .responseMimeType(
                                        "application/json"
                                )
                                .build()
                );

        // 5. Return Gemini's JSON response
        return response.text();
    }
}
package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.*;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.ImageAuthenticity;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import com.civicsense.complaint.entity.classification.ComplaintDepartment;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.stream.Collectors;

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

        String categories =
                Arrays.stream(ComplaintCategory.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

        String severities =
                Arrays.stream(ComplaintSeverity.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

        String departments =
                Arrays.stream(ComplaintDepartment.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

        String authenticities =
                Arrays.stream(ImageAuthenticity.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

        String consistencies =
                Arrays.stream(DescriptionImageConsistency.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));


        // 2. Prepare prompt
        String prompt = """
        Analyze this civic complaint using BOTH the
        complaint description and the uploaded image.

        Complaint description:
        %s

        Analyze:

        Analyze:
        
        1. Detect the language.
        2. Generate a short, clear complaint title.
        3. Classify the complaint category.
        4. Determine severity.
        5. Create a short summary.
        6. Recommend the responsible civic department.
        7. Give a confidence score between 0 and 1.
        8. Assess whether the image appears likely real,
           suspicious, or uncertain.
        9. Check whether the image is consistent with
           the complaint description.

        CATEGORY:
        The category MUST be exactly ONE of:
        %s

        SEVERITY:
        The severity MUST be exactly ONE of:
        %s

        DEPARTMENT:
        The department MUST be exactly ONE of:
        %s

        IMAGE AUTHENTICITY:
        The imageAuthenticity MUST be exactly ONE of:
        %s

        DESCRIPTION-IMAGE CONSISTENCY:
        The descriptionImageConsistency MUST be exactly ONE of:
        %s

        IMPORTANT:
        - Do not create new category values.
        - Do not create new severity values.
        - Do not create new department values.
        - Do not create new authenticity values.
        - Do not create new consistency values.
        - Use the values exactly as provided.
        - confidence must be a number between 0 and 1.
        - Return ONLY valid JSON.
        - Do not wrap the JSON in markdown or code fences.
        - title must be concise and describe the main civic issue.
        - title should normally be 5 to 10 words.

        Required JSON fields:

        language
        title
        category
        severity
        summary
        department
        confidence
        imageAuthenticity
        descriptionImageConsistency
        """.formatted(
                complaint.getDescription(),
                categories,
                severities,
                departments,
                authenticities,
                consistencies
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
                        "gemini-3.6-flash",
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
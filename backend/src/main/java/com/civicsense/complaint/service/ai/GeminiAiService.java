package com.civicsense.complaint.service.ai;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
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

    public String verifyDuplicateComplaints(
            Complaint complaint1,
            ComplaintImage image1,
            ComplaintAnalysis analysis1,
            Complaint complaint2,
            ComplaintImage image2,
            ComplaintAnalysis analysis2,
            double titleSimilarity,
            double descriptionSimilarity,
            double summarySimilarity,
            double imageSimilarity,
            double addressSimilarity,
            double distanceMeters
    ) {

        // 1. Download first complaint image
        byte[] imageBytes1 = restClient
                .get()
                .uri(image1.getImageUrl())
                .retrieve()
                .body(byte[].class);

        if (imageBytes1 == null || imageBytes1.length == 0) {
            throw new IllegalStateException(
                    "Unable to download first complaint image"
            );
        }

        // 2. Download second complaint image
        byte[] imageBytes2 = restClient
                .get()
                .uri(image2.getImageUrl())
                .retrieve()
                .body(byte[].class);

        if (imageBytes2 == null || imageBytes2.length == 0) {
            throw new IllegalStateException(
                    "Unable to download second complaint image"
            );
        }

        // 3. Prepare verification prompt
        String prompt = """
        Determine whether these two civic complaints
        represent the SAME real-world civic incident
        or DIFFERENT civic incidents.

        This is a duplicate verification task.

        ==============================
        COMPLAINT 1
        ==============================

        Title: %s
        Description: %s
        AI Summary: %s
        Category: %s
        Address: %s
        Latitude: %s
        Longitude: %s


        ==============================
        COMPLAINT 2
        ==============================

        Title: %s
        Description: %s
        AI Summary: %s
        Category: %s
        Address: %s
        Latitude: %s
        Longitude: %s


        ==============================
        STAGE 1 SIMILARITY EVIDENCE
        ==============================

        The first-stage duplicate detection system
        calculated the following similarities:

        Title similarity: %s
        Description similarity: %s
        Summary similarity: %s
        Image similarity: %s
        Address similarity: %s
        Geographic distance: %s meters

        These values are SUPPORTING EVIDENCE only.

        Do NOT treat a high similarity score as proof
        that the complaints represent the same incident.

        You must independently evaluate the complaints
        and BOTH images before making the final decision.


        ==============================
        VISUAL VERIFICATION
        ==============================

        You will receive two images:

        IMAGE 1 belongs to Complaint 1.
        IMAGE 2 belongs to Complaint 2.

        Carefully compare BOTH images.

        Consider:

        - Whether both images show the same physical civic issue.
        - Visual surroundings and background.
        - Buildings, roads, walls, poles and landmarks.
        - Vehicles, containers, signs and other identifiable objects.
        - Shape, size and appearance of the reported issue.
        - Whether the physical environment appears to be the same.
        - Whether the images could have been captured at
          different times at the same location.
        - Whether the descriptions refer to the same physical incident.
        - Whether the addresses and coordinates are consistent.
        - Whether the complaints could instead represent
          two separate issues of the same category.


        ==============================
        IMPORTANT DISTINCTION
        ==============================

        Distinguish between:

        1. Same type of civic issue
        2. Same real-world civic incident

        Your task is specifically to determine
        whether they represent the SAME real-world incident.

        For example:

        Two garbage piles in the same neighborhood
        are NOT necessarily the same incident.

        Two potholes on the same road
        are NOT necessarily the same incident.

        Similar category, similar wording, nearby coordinates,
        or high embedding similarity alone are NOT sufficient.


        ==============================
        VERDICT
        ==============================

        The verdict MUST be exactly ONE of:

        SAME
        DIFFERENT
        UNCERTAIN

        Use SAME only when the combined evidence strongly
        indicates that both complaints refer to the same
        physical real-world incident.

        Use DIFFERENT when the evidence indicates that
        they are separate physical incidents.

        Use UNCERTAIN when the evidence is insufficient,
        ambiguous, or contradictory.


        ==============================
        CONFIDENCE
        ==============================

        Return a number between 0 and 1.

        Confidence must represent your confidence in the
        final verdict, not merely the similarity of the
        complaints.


        ==============================
        REASONING
        ==============================

        Provide concise reasoning explaining the main
        evidence behind the verdict.

        Specifically mention important visual differences
        or similarities when relevant.


        ==============================
        OUTPUT
        ==============================

        IMPORTANT:

        - Do not invent information.
        - Do not assume nearby locations mean the same incident.
        - Do not assume similar descriptions mean the same incident.
        - Do not assume similar categories mean the same incident.
        - Do not blindly follow Stage 1.
        - Independently inspect both images.
        - Return ONLY valid JSON.
        - Do not wrap the JSON in markdown or code fences.

        Required JSON fields:

        verdict
        confidence
        reasoning

        """.formatted(
                // Complaint 1
                complaint1.getTitle(),
                complaint1.getDescription(),
                analysis1.getSummary(),
                analysis1.getCategory() != null ? analysis1.getCategory().name() : complaint1.getCategory(),
                complaint1.getAddress(),
                complaint1.getLatitude(),
                complaint1.getLongitude(),

                // Complaint 2
                complaint2.getTitle(),
                complaint2.getDescription(),
                analysis2.getSummary(),
                analysis2.getCategory() != null ? analysis2.getCategory().name() : complaint2.getCategory(),
                complaint2.getAddress(),
                complaint2.getLatitude(),
                complaint2.getLongitude(),

                // Stage 1 evidence
                titleSimilarity,
                descriptionSimilarity,
                summarySimilarity,
                imageSimilarity,
                addressSimilarity,
                distanceMeters
        );

        // 4. Create multimodal content
        Content content =
                Content.fromParts(
                        Part.fromText(prompt),

                        Part.fromText(
                                "IMAGE FOR COMPLAINT 1:"
                        ),
                        Part.fromBytes(
                                imageBytes1,
                                image1.getContentType()
                        ),

                        Part.fromText(
                                "IMAGE FOR COMPLAINT 2:"
                        ),
                        Part.fromBytes(
                                imageBytes2,
                                image2.getContentType()
                        )
                );

        // 5. Send request to Gemini
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

        // 6. Return Gemini verification result
        return response.text();
    }

}
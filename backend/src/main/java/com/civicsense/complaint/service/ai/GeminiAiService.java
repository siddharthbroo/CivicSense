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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiAiService {

        private final Client geminiClient;

        private final RestClient restClient = RestClient.create();

        public String analyzeComplaint(
                        Complaint complaint,
                        ComplaintImage complaintImage) {

                String categories = Arrays.stream(ComplaintCategory.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));

                String severities = Arrays.stream(ComplaintSeverity.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));

                String departments = Arrays.stream(ComplaintDepartment.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));

                String authenticities = Arrays.stream(ImageAuthenticity.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));

                String consistencies = Arrays.stream(DescriptionImageConsistency.values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));

                // Check if photo is present
                boolean hasImage = complaintImage != null && complaintImage.getImageUrl() != null && !complaintImage.getImageUrl().isBlank();

                byte[] imageBytes = null;
                if (hasImage) {
                        try {
                                imageBytes = restClient
                                                .get()
                                                .uri(complaintImage.getImageUrl())
                                                .retrieve()
                                                .body(byte[].class);
                        } catch (Exception e) {
                                System.out.println("Could not download image from storage: " + e.getMessage());
                                hasImage = false;
                        }
                }

                String prompt;
                if (hasImage && imageBytes != null && imageBytes.length > 0) {
                        prompt = """
                                Analyze this civic complaint using BOTH the
                                complaint description and the uploaded image.

                                Complaint description:
                                %s

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
                                10. Assess the overall safety risk.
                                11. Determine whether there is an immediate hazard requiring urgent attention.
                                12. Assess the public impact on citizens, students, and commuters.
                                13. Explain the potential harm that could occur if left unaddressed.
                                14. Recommend the response urgency.

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

                                SAFETY RISK:
                                The safetyRisk MUST be exactly ONE of:
                                %s

                                IMMEDIATE HAZARD:
                                The immediateHazard MUST be a boolean: true or false.

                                PUBLIC IMPACT:
                                The publicImpact MUST be exactly ONE of:
                                %s

                                POTENTIAL HARM:
                                A concise 1-2 sentence explanation of potential harm to people, vehicles, or property.

                                RECOMMENDED URGENCY:
                                The recommendedUrgency MUST be exactly ONE of:
                                %s

                                IMPORTANT:
                                - Do not create new category values.
                                - Do not create new severity values.
                                - Do not create new department values.
                                - Do not create new authenticity values.
                                - Do not create new consistency values.
                                - Do not create new safetyRisk values.
                                - Do not create new publicImpact values.
                                - Do not create new recommendedUrgency values.
                                - Use the values exactly as provided.
                                - confidence must be a number between 0 and 1.
                                - immediateHazard must be boolean true or false.
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
                                safetyRisk
                                immediateHazard
                                publicImpact
                                potentialHarm
                                recommendedUrgency
                                """.formatted(
                                complaint.getDescription(),
                                categories,
                                severities,
                                departments,
                                authenticities,
                                consistencies,
                                severities,
                                severities,
                                severities);
                } else {
                        // Text-only analysis when no photo was uploaded
                        prompt = """
                                Analyze this civic complaint using the complaint description.
                                No photo was provided for this complaint (text-only submission).

                                Complaint description:
                                %s

                                Analyze:

                                1. Detect the language.
                                2. Generate a short, clear complaint title.
                                3. Classify the complaint category.
                                4. Determine severity.
                                5. Create a short summary.
                                6. Recommend the responsible civic department.
                                7. Give a confidence score between 0 and 1.
                                8. Assess the overall safety risk based on description.
                                9. Determine whether there is an immediate hazard requiring urgent attention.
                                10. Assess the public impact on citizens, students, and commuters.
                                11. Explain the potential harm that could occur if left unaddressed.
                                12. Recommend the response urgency.

                                CATEGORY:
                                The category MUST be exactly ONE of:
                                %s

                                SEVERITY:
                                The severity MUST be exactly ONE of:
                                %s

                                DEPARTMENT:
                                The department MUST be exactly ONE of:
                                %s

                                SAFETY RISK:
                                The safetyRisk MUST be exactly ONE of:
                                %s

                                IMMEDIATE HAZARD:
                                The immediateHazard MUST be a boolean: true or false.

                                PUBLIC IMPACT:
                                The publicImpact MUST be exactly ONE of:
                                %s

                                POTENTIAL HARM:
                                A concise 1-2 sentence explanation of potential harm to people, vehicles, or property.

                                RECOMMENDED URGENCY:
                                The recommendedUrgency MUST be exactly ONE of:
                                %s

                                IMPORTANT:
                                - Do not create new category values.
                                - Do not create new severity values.
                                - Do not create new department values.
                                - Do not create new safetyRisk values.
                                - Do not create new publicImpact values.
                                - Do not create new recommendedUrgency values.
                                - Use the values exactly as provided.
                                - Set imageAuthenticity to "NOT_APPLICABLE" (no image submitted).
                                - Set descriptionImageConsistency to "NOT_APPLICABLE" (no image submitted).
                                - confidence must be a number between 0 and 1.
                                - immediateHazard must be boolean true or false.
                                - Return ONLY valid JSON.
                                - Do not wrap the JSON in markdown or code fences.
                                - title must be concise and describe the main civic issue (5 to 10 words).

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
                                safetyRisk
                                immediateHazard
                                publicImpact
                                potentialHarm
                                recommendedUrgency
                                """.formatted(
                                complaint.getDescription(),
                                categories,
                                severities,
                                departments,
                                severities,
                                severities,
                                severities);
                }

                Content content;
                if (hasImage && imageBytes != null && imageBytes.length > 0) {
                        content = Content.fromParts(
                                        Part.fromText(prompt),
                                        Part.fromBytes(
                                                        imageBytes,
                                                        complaintImage.getContentType() != null ? complaintImage.getContentType() : "image/jpeg"));
                } else {
                        content = Content.fromParts(Part.fromText(prompt));
                }

                GenerateContentResponse response = geminiClient.models.generateContent(
                                "gemini-3.6-flash",
                                content,
                                GenerateContentConfig.builder()
                                                .responseMimeType("application/json")
                                                .build());

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
                        double distanceMeters) {

                byte[] imageBytes1 = null;
                if (image1 != null && image1.getImageUrl() != null && !image1.getImageUrl().isBlank()) {
                        try {
                                imageBytes1 = restClient.get().uri(image1.getImageUrl()).retrieve().body(byte[].class);
                        } catch (Exception e) {
                                System.out.println("Could not download image1: " + e.getMessage());
                        }
                }

                byte[] imageBytes2 = null;
                if (image2 != null && image2.getImageUrl() != null && !image2.getImageUrl().isBlank()) {
                        try {
                                imageBytes2 = restClient.get().uri(image2.getImageUrl()).retrieve().body(byte[].class);
                        } catch (Exception e) {
                                System.out.println("Could not download image2: " + e.getMessage());
                        }
                }

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
                                and any available images before making the final decision.


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


                                ==============================
                                REASONING
                                ==============================

                                Provide concise reasoning explaining the main
                                evidence behind the verdict.


                                ==============================
                                OUTPUT
                                ==============================

                                Return ONLY valid JSON.
                                Do not wrap the JSON in markdown or code fences.

                                Required JSON fields:

                                verdict
                                confidence
                                reasoning

                                """.formatted(
                                complaint1.getTitle(),
                                complaint1.getDescription(),
                                analysis1.getSummary(),
                                analysis1.getCategory() != null ? analysis1.getCategory().name() : complaint1.getCategory(),
                                complaint1.getAddress(),
                                complaint1.getLatitude(),
                                complaint1.getLongitude(),

                                complaint2.getTitle(),
                                complaint2.getDescription(),
                                analysis2.getSummary(),
                                analysis2.getCategory() != null ? analysis2.getCategory().name() : complaint2.getCategory(),
                                complaint2.getAddress(),
                                complaint2.getLatitude(),
                                complaint2.getLongitude(),

                                titleSimilarity,
                                descriptionSimilarity,
                                summarySimilarity,
                                imageSimilarity,
                                addressSimilarity,
                                distanceMeters);

                List<Part> parts = new ArrayList<>();
                parts.add(Part.fromText(prompt));

                if (imageBytes1 != null && imageBytes1.length > 0 && image1 != null) {
                        parts.add(Part.fromText("IMAGE FOR COMPLAINT 1:"));
                        parts.add(Part.fromBytes(imageBytes1, image1.getContentType() != null ? image1.getContentType() : "image/jpeg"));
                }

                if (imageBytes2 != null && imageBytes2.length > 0 && image2 != null) {
                        parts.add(Part.fromText("IMAGE FOR COMPLAINT 2:"));
                        parts.add(Part.fromBytes(imageBytes2, image2.getContentType() != null ? image2.getContentType() : "image/jpeg"));
                }

                Content content = Content.fromParts(parts.toArray(new Part[0]));

                GenerateContentResponse response = geminiClient.models.generateContent(
                                "gemini-3.6-flash",
                                content,
                                GenerateContentConfig.builder()
                                                .responseMimeType("application/json")
                                                .build());

                return response.text();
        }
}
package com.civicsense.complaint.service.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageModerationService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ACCEPTED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final Client geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates file size, content-type, magic bytes, and decodability.
     */
    public ImageModerationResult validateFileIntegrity(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ImageModerationResult.invalidFile("Image file is missing or empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ImageModerationResult.invalidFile("Image size exceeds maximum allowed limit of 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ImageModerationResult.invalidFile("Only JPG, JPEG, and PNG images are supported.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 12) {
                return ImageModerationResult.invalidFile("File header is too small to be a valid image.");
            }

            // Verify magic numbers
            boolean isJpeg = (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF);
            boolean isPng = (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47);

            if (!isJpeg && !isPng) {
                return ImageModerationResult.invalidFile("File content does not match JPEG or PNG signature.");
            }

            // Verify decodability with ImageIO
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                BufferedImage img = ImageIO.read(is);
                if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
                    return ImageModerationResult.invalidFile("Image is corrupted or unreadable.");
                }
            }

            return ImageModerationResult.approved("File integrity check passed.", "SAFE");
        } catch (IOException e) {
            return ImageModerationResult.invalidFile("Failed to read image bytes: " + e.getMessage());
        }
    }

    /**
     * Evaluates image safety AND complaint relevance using Gemini multimodal pre-screen.
     * Enforces:
     * 1. Safety Policy: Prohibits explicit nudity, CSAM, hate group insignia, severe violence/doxxing.
     *    Protects legitimate unpleasant civic photographs (sewage, garbage dumps, dead stray pests, accidents, fires).
     * 2. Complaint Relevance:
     *    - RELEVANT: clearly shows or relates to the civic problem.
     *    - POTENTIALLY_RELEVANT: context relates to the environment/location, even if exact damage isn't immediately obvious.
     *    - UNRELATED: completely non-civic or unrelated (e.g. cat/pet photo, celebrity portrait, food, party meme for road damage).
     *    - UNCERTAIN: AI cannot confidently determine relevance.
     */
    public ImageModerationResult evaluateImageSafetyAndRelevance(
            byte[] imageBytes,
            String contentType,
            String category,
            String description
    ) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ImageModerationResult.invalidFile("Image bytes are empty.");
        }

        String complaintCategory = (category != null && !category.isBlank()) ? category.trim() : "GENERAL_CIVIC_ISSUE";
        String complaintDesc = (description != null && !description.isBlank()) ? description.trim() : "No description provided yet.";

        String prompt = """
                You are a content safety and relevance evaluator for CivicSense, a municipal civic complaint platform.
                Your job is to screen uploaded images BEFORE they are stored or processed for:
                1. CONTENT SAFETY (Policy compliance)
                2. COMPLAINT RELEVANCE (Relation to the civic complaint)

                COMPLAINT CONTEXT:
                - Selected Category: %s
                - Complaint Description: %s

                -----------------------------------------
                PART 1: CONTENT SAFETY RULES
                -----------------------------------------
                CIVIC EXCEPTION RULES:
                Civic complaints often contain unpleasant, dirty, or graphic civic imagery that IS LEGITIMATE.
                DO NOT prohibit legitimate civic issues such as:
                - Sewage overflows, dirty water, mud, sludge
                - Garbage dumps, landfill, rotten waste, litter
                - Road damage, broken asphalt, open manholes
                - Dead birds, rats, or stray animals on roads/in public spaces (civic carcass removal)
                - Small fires, smoke, trash burning, burnt debris
                - Minor road accident damage (dented vehicles, fallen poles)

                STRICTLY PROHIBITED CONTENT:
                Classify as PROHIBITED only if the image contains with high confidence:
                1. EXPLICIT_NUDITY: Pornography, genitalia, sexual acts, or sexual violence.
                2. HATE_EXTREMISM: Hate group insignia, Nazi swastikas, terrorist symbols, hate propaganda.
                3. HARASSMENT_DOXXING: Government ID cards (Aadhaar, passport, PAN), credit cards, or personal defamation.
                4. SEVERE_NON_CIVIC_VIOLENCE: Graphic human mutilation, decapitation, or human torture.

                -----------------------------------------
                PART 2: COMPLAINT RELEVANCE RULES
                -----------------------------------------
                - "RELEVANT": The image depicts or clearly relates to the reported civic issue or category.
                - "POTENTIALLY_RELEVANT": The image shows a public/outdoor civic context (street, pavement, building, drain) consistent with the location/context, even if the specific fault is difficult to spot. DO NOT reject these.
                - "UNRELATED": The image is CLEARLY UNRELATED to any civic complaint or the reported issue (for example: a pet cat/dog photo, animal portrait, food plate, selfie, fashion photo, cartoon/meme, screenshot of a mobile chat).
                - "UNCERTAIN": The image is blurry, ambiguous, or the AI cannot determine relevance with high confidence.

                -----------------------------------------
                OVERALL VERDICT:
                - If safety is violated -> verdict: "PROHIBITED"
                - If safety is clean but image is clearly unrelated (e.g. cat photo for road damage) -> verdict: "UNRELATED"
                - If safety is uncertain OR relevance is uncertain -> verdict: "QUARANTINE"
                - If safety is clean AND (relevant or potentially relevant) -> verdict: "SAFE"

                Output format: Return ONLY valid JSON with no markdown formatting:
                {
                  "verdict": "SAFE" | "PROHIBITED" | "UNRELATED" | "QUARANTINE",
                  "category": "SAFE" | "EXPLICIT_NUDITY" | "HATE_EXTREMISM" | "HARASSMENT_DOXXING" | "SEVERE_NON_CIVIC_VIOLENCE" | "UNRELATED_CONTENT" | "UNCERTAIN",
                  "relevanceStatus": "RELEVANT" | "POTENTIALLY_RELEVANT" | "UNRELATED" | "UNCERTAIN",
                  "confidence": <number between 0.0 and 1.0>,
                  "reason": "<short explanation under 20 words>"
                }
                """.formatted(complaintCategory, complaintDesc);

        try {
            Content content = Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(imageBytes, contentType != null ? contentType : "image/jpeg")
            );

            GenerateContentResponse response = geminiClient.models.generateContent(
                    "gemini-3.6-flash",
                    content,
                    GenerateContentConfig.builder()
                            .responseMimeType("application/json")
                            .build()
            );

            String text = response.text();
            if (text == null || text.isBlank()) {
                return ImageModerationResult.quarantined(
                        "Image verification returned empty response. Quarantined for review.",
                        "MODERATION_UNAVAILABLE",
                        "UNCERTAIN",
                        0.5
                );
            }

            JsonNode node = objectMapper.readTree(text);
            String verdict = node.path("verdict").asText("SAFE").toUpperCase();
            String categoryResult = node.path("category").asText("SAFE");
            String relevanceStatus = node.path("relevanceStatus").asText("RELEVANT").toUpperCase();
            double confidence = node.path("confidence").asDouble(0.8);
            String reason = node.path("reason").asText("");

            if ("PROHIBITED".equals(verdict) && confidence >= 0.70) {
                return ImageModerationResult.prohibited(
                        reason.isBlank() ? "Image violates civic safety policies (" + categoryResult + ")." : reason,
                        categoryResult,
                        confidence
                );
            } else if ("UNRELATED".equals(verdict) || "UNRELATED".equals(relevanceStatus)) {
                return ImageModerationResult.unrelated(
                        reason.isBlank() ? "This photo appears unrelated to the selected civic complaint. Please attach a photo of the civic issue." : reason,
                        confidence
                );
            } else if ("QUARANTINE".equals(verdict) || "UNCERTAIN".equals(relevanceStatus) || ("PROHIBITED".equals(verdict) && confidence < 0.70)) {
                return ImageModerationResult.quarantined(
                        reason.isBlank() ? "Image flagged for administrative review." : reason,
                        categoryResult,
                        relevanceStatus,
                        confidence
                );
            } else {
                return ImageModerationResult.approved(
                        reason.isBlank() ? "Image approved for civic submission." : reason,
                        relevanceStatus
                );
            }

        } catch (Exception e) {
            System.err.println("Gemini Image Moderation/Relevance check unavailable: " + e.getMessage());
            return ImageModerationResult.quarantined(
                    "Image verification service is temporarily unavailable. Image quarantined for review.",
                    "MODERATION_FAILED",
                    "UNCERTAIN",
                    0.0
            );
        }
    }

    /**
     * Backward-compatible evaluation without context parameters.
     */
    public ImageModerationResult evaluateImageSafety(byte[] imageBytes, String contentType) {
        return evaluateImageSafetyAndRelevance(imageBytes, contentType, null, null);
    }

    /**
     * Complete evaluation pipeline for uploaded image including file integrity, safety, and relevance:
     */
    public ImageModerationResult validateAndModerate(MultipartFile file, String category, String description) {
        ImageModerationResult integrity = validateFileIntegrity(file);
        if (!integrity.valid()) {
            return integrity;
        }

        try {
            return evaluateImageSafetyAndRelevance(file.getBytes(), file.getContentType(), category, description);
        } catch (IOException e) {
            return ImageModerationResult.invalidFile("Failed to read image bytes: " + e.getMessage());
        }
    }

    public ImageModerationResult validateAndModerate(MultipartFile file) {
        return validateAndModerate(file, null, null);
    }
}

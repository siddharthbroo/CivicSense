package com.civicsense.complaint.service.image;

import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageModerationServiceTest {

    @Mock
    private Client geminiClient;

    @InjectMocks
    private ImageModerationService imageModerationService;

    private byte[] validJpegBytes;

    @BeforeEach
    void setUp() throws IOException {
        // Generate valid in-memory JPEG bytes
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        validJpegBytes = baos.toByteArray();
    }

    @Test
    @DisplayName("Should accept valid JPEG image file with proper magic bytes and decodability")
    void testValidateFileIntegrity_ValidJpeg() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "pothole.jpg", "image/jpeg", validJpegBytes
        );

        ImageModerationResult result = imageModerationService.validateFileIntegrity(file);

        assertTrue(result.valid());
        assertFalse(result.prohibited());
        assertEquals("SAFE", result.category());
    }

    @Test
    @DisplayName("Should reject empty or null image file")
    void testValidateFileIntegrity_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[0]
        );

        ImageModerationResult result = imageModerationService.validateFileIntegrity(emptyFile);

        assertFalse(result.valid());
        assertEquals("INVALID_FILE", result.category());
    }

    @Test
    @DisplayName("Should reject oversized image exceeding 10MB")
    void testValidateFileIntegrity_OversizedFile() {
        // 11MB fake bytes with valid header
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;

        MockMultipartFile file = new MockMultipartFile(
                "image", "huge.jpg", "image/jpeg", largeBytes
        );

        ImageModerationResult result = imageModerationService.validateFileIntegrity(file);

        assertFalse(result.valid());
        assertTrue(result.reason().contains("10MB"));
    }

    @Test
    @DisplayName("Should reject unsupported content types such as PDF or GIF")
    void testValidateFileIntegrity_UnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "document.pdf", "application/pdf", new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}
        );

        ImageModerationResult result = imageModerationService.validateFileIntegrity(file);

        assertFalse(result.valid());
        assertTrue(result.reason().contains("Only JPG, JPEG, and PNG"));
    }

    @Test
    @DisplayName("Should reject file whose content does not match JPEG or PNG magic bytes")
    void testValidateFileIntegrity_FakeImageExtension() {
        byte[] fakeBytes = "This is a plain text file masquerading as an image.".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image", "spoofed.jpg", "image/jpeg", fakeBytes
        );

        ImageModerationResult result = imageModerationService.validateFileIntegrity(file);

        assertFalse(result.valid());
        assertTrue(result.reason().contains("does not match JPEG or PNG signature"));
    }

    @Test
    @DisplayName("Should safely quarantine image if moderation service fails (fail-safe policy)")
    void testEvaluateImageSafety_FailSafeQuarantineOnServiceFailure() {
        // When Gemini throws an exception or fails, image must be quarantined, NOT approved
        ImageModerationResult result = imageModerationService.evaluateImageSafety(validJpegBytes, "image/jpeg");

        assertNotNull(result);
        assertTrue(result.quarantined(), "Fail-safe: image must be quarantined when moderation fails");
        assertFalse(result.prohibited());
        assertEquals("MODERATION_FAILED", result.category());
    }

    @Test
    @DisplayName("Should quarantine image if relevance and safety evaluation fails")
    void testEvaluateImageSafetyAndRelevance_FailSafeQuarantineOnServiceFailure() {
        ImageModerationResult result = imageModerationService.evaluateImageSafetyAndRelevance(
                validJpegBytes, "image/jpeg", "ROAD_DAMAGE", "Huge pothole on 5th main"
        );

        assertNotNull(result);
        assertTrue(result.quarantined());
        assertFalse(result.prohibited());
        assertFalse(result.unrelated());
        assertEquals("UNCERTAIN", result.relevanceStatus());
    }

    @Test
    @DisplayName("Should create unrelated ImageModerationResult correctly")
    void testImageModerationResult_Unrelated() {
        ImageModerationResult result = ImageModerationResult.unrelated("Image appears to be a cat photo.", 0.95);

        assertFalse(result.valid());
        assertFalse(result.prohibited());
        assertTrue(result.unrelated());
        assertFalse(result.quarantined());
        assertEquals("UNRELATED", result.relevanceStatus());
        assertEquals("UNRELATED_CONTENT", result.category());
        assertEquals(0.95, result.confidence());
    }
}


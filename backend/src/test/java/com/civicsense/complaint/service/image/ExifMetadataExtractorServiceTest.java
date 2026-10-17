package com.civicsense.complaint.service.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ExifMetadataExtractorServiceTest {

    private ExifMetadataExtractorService extractorService;

    @BeforeEach
    void setUp() {
        extractorService = new ExifMetadataExtractorService();
    }

    @Test
    @DisplayName("Should return empty ExifMetadata when file is null")
    void testNullFileReturnsEmpty() {
        ExifMetadata metadata = extractorService.extractMetadata(null, 28.6139, 77.2090);
        assertNotNull(metadata);
        assertFalse(metadata.hasGps());
        assertNull(metadata.latitude());
        assertNull(metadata.longitude());
        assertFalse(metadata.locationMismatchFlag());
    }

    @Test
    @DisplayName("Should return empty ExifMetadata when file is empty")
    void testEmptyFileReturnsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);
        ExifMetadata metadata = extractorService.extractMetadata(emptyFile, 28.6139, 77.2090);
        assertNotNull(metadata);
        assertFalse(metadata.hasGps());
        assertNull(metadata.latitude());
    }

    @Test
    @DisplayName("Should gracefully handle non-EXIF or plain image without crashing")
    void testPlainImageDoesNotCrash() {
        byte[] dummyJpg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", dummyJpg);

        ExifMetadata metadata = extractorService.extractMetadata(mockFile, 28.6139, 77.2090);
        assertNotNull(metadata);
        assertFalse(metadata.hasGps());
        assertFalse(metadata.locationMismatchFlag());
    }
}
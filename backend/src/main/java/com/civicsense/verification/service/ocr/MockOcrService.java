package com.civicsense.verification.service.ocr;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;


public class MockOcrService implements OcrService {

    @Override
    public OcrResult extractDetails(MultipartFile document) {

        // Temporary mock data for development.
        // Real OCR implementation will replace this later.

        return new OcrResult(
                "Test User",
                LocalDate.of(2000, 1, 1),
                "MALE"
        );
    }
}
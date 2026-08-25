package com.civicsense.verification.service.ocr;

import java.time.LocalDate;

public record OcrResult(
        String name,
        LocalDate dateOfBirth,
        String gender
) {
}
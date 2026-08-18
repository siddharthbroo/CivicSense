package com.civicsense.verification.service.ocr;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    OcrResult extractDetails(MultipartFile document);
}
package com.civicsense.verification.service;

import com.civicsense.verification.dto.IdentityConfirmationRequest;
import com.civicsense.verification.dto.IdentityDocumentUploadResponse;
import com.civicsense.verification.dto.IdentityVerificationResponse;
import com.civicsense.verification.entity.DocumentType;
import com.civicsense.verification.entity.IdentityVerification;
import com.civicsense.verification.entity.OCRStatus;
import com.civicsense.verification.entity.VerificationStatus;
import com.civicsense.verification.repository.IdentityVerificationRepository;
import com.civicsense.verification.service.ocr.OcrResult;
import com.civicsense.verification.service.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class IdentityVerificationServiceImpl
        implements IdentityVerificationService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final OcrService ocrService;
    private final IdentityVerificationRepository identityVerificationRepository;

    @Override
    public IdentityDocumentUploadResponse uploadDocument(
            MultipartFile document,
            DocumentType documentType
    ) {

        // 1. Validate uploaded file
        validateDocument(document);

        // 2. Only Aadhaar is currently supported
        if (documentType != DocumentType.AADHAAR) {
            throw new IllegalArgumentException(
                    "Only Aadhaar documents are currently supported"
            );
        }

        // 3. Process document using OCR
        OcrResult ocrResult =
                ocrService.extractDetails(document);

        // 4. Create identity verification record
        IdentityVerification verification =
                new IdentityVerification();

        verification.setDocumentType(documentType);

        // Store original OCR output
        verification.setExtractedName(
                ocrResult.name()
        );

        verification.setExtractedDob(
                ocrResult.dateOfBirth()
        );

        verification.setExtractedGender(
                ocrResult.gender()
        );

        // OCR completed successfully
        verification.setOcrStatus(
                OCRStatus.COMPLETED
        );

        // User has not reviewed the OCR data yet
        verification.setVerificationStatus(
                VerificationStatus.PENDING
        );

        // 5. Save verification record
        IdentityVerification savedVerification =
                identityVerificationRepository.save(
                        verification
                );

        // 6. Return OCR result to frontend
        return new IdentityDocumentUploadResponse(
                savedVerification.getId(),
                savedVerification.getDocumentType().name(),
                savedVerification.getOcrStatus(),
                savedVerification.getExtractedName(),
                savedVerification.getExtractedDob(),
                savedVerification.getExtractedGender()
        );
    }

    @Override
    public IdentityVerificationResponse confirmIdentity(
            IdentityConfirmationRequest request
    ) {

        // 1. Find identity verification record
        IdentityVerification verification =
                identityVerificationRepository.findById(
                        request.verificationId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Identity verification not found"
                        )
                );

        // 2. Save user-confirmed details
        // Do NOT overwrite original OCR result.
        verification.setConfirmedName(
                request.name()
        );

        verification.setConfirmedDob(
                request.dateOfBirth()
        );

        verification.setConfirmedGender(
                request.gender()
        );

        // 3. User has reviewed and confirmed the details
        verification.setVerificationStatus(
                VerificationStatus.USER_CONFIRMED
        );

        // 4. Save updated verification record
        IdentityVerification savedVerification =
                identityVerificationRepository.save(
                        verification
                );

        // 5. Return confirmation response
        return new IdentityVerificationResponse(
                savedVerification.getId(),
                savedVerification.getOcrStatus(),
                savedVerification.getVerificationStatus(),
                "Identity details confirmed successfully"
        );
    }

    private void validateDocument(MultipartFile document) {

        if (document == null || document.isEmpty()) {
            throw new IllegalArgumentException(
                    "Document file is required"
            );
        }

        if (document.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Document size must not exceed 5 MB"
            );
        }

        String contentType =
                document.getContentType();

        if (contentType == null ||
                (!contentType.equalsIgnoreCase("image/jpeg")
                        && !contentType.equalsIgnoreCase("image/png"))) {

            throw new IllegalArgumentException(
                    "Only JPG, JPEG and PNG images are supported"
            );
        }
    }
}
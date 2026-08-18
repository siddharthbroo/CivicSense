package com.civicsense.verification.service;

import com.civicsense.verification.dto.IdentityDocumentUploadResponse;
import com.civicsense.verification.dto.IdentityVerificationResponse;
import com.civicsense.verification.entity.DocumentType;
import com.civicsense.verification.entity.IdentityVerification;
import com.civicsense.verification.repository.IdentityVerificationRepository;
import com.civicsense.verification.service.ocr.OcrResult;
import com.civicsense.verification.service.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.civicsense.verification.entity.OCRStatus;
import com.civicsense.verification.entity.VerificationStatus;
import com.civicsense.verification.dto.IdentityConfirmationRequest;

@Service
@RequiredArgsConstructor
public class IdentityVerificationServiceImpl
        implements IdentityVerificationService {

    private final OcrService ocrService;
    private final IdentityVerificationRepository identityVerificationRepository;

    @Override
    public IdentityDocumentUploadResponse uploadDocument(
            MultipartFile document,
            DocumentType documentType
    ) {

        // 1. OCR processing
        OcrResult ocrResult = ocrService.extractDetails(document);

        // 2. Create identity verification record
        IdentityVerification verification = new IdentityVerification();

        verification.setDocumentType(documentType);
        verification.setExtractedName(ocrResult.name());
        verification.setExtractedDob(ocrResult.dateOfBirth());
        verification.setExtractedGender(ocrResult.gender());

        verification.setOcrStatus(OCRStatus.COMPLETED);
        verification.setVerificationStatus(VerificationStatus.PENDING);

        // 3. Save verification
        IdentityVerification savedVerification =
                identityVerificationRepository.save(verification);

        // 4. Return response to frontend
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

        IdentityVerification verification =
                identityVerificationRepository.findById(request.verificationId())
                        .orElseThrow(() ->
                                new RuntimeException("Identity verification not found")
                        );

        verification.setConfirmedName(request.name());
        verification.setConfirmedDob(request.dateOfBirth());
        verification.setConfirmedGender(request.gender());

        verification.setVerificationStatus(VerificationStatus.VERIFIED);

        IdentityVerification savedVerification =
                identityVerificationRepository.save(verification);

        return new IdentityVerificationResponse(
                savedVerification.getId(),
                savedVerification.getOcrStatus(),
                savedVerification.getVerificationStatus(),
                "Identity details confirmed successfully"
        );
    }
}
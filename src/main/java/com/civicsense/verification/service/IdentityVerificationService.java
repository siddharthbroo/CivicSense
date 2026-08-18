package com.civicsense.verification.service;

import com.civicsense.verification.dto.IdentityConfirmationRequest;
import com.civicsense.verification.dto.IdentityDocumentUploadResponse;
import com.civicsense.verification.dto.IdentityVerificationResponse;
import com.civicsense.verification.entity.DocumentType;
import org.springframework.web.multipart.MultipartFile;

public interface IdentityVerificationService {

    IdentityDocumentUploadResponse uploadDocument(
            MultipartFile document,
            DocumentType documentType
    );
    IdentityVerificationResponse confirmIdentity(
            IdentityConfirmationRequest request
    );
}
package com.civicsense.verification.controller;

import com.civicsense.verification.dto.IdentityConfirmationRequest;
import com.civicsense.verification.dto.IdentityDocumentUploadResponse;
import com.civicsense.verification.dto.IdentityVerificationResponse;
import com.civicsense.verification.entity.DocumentType;
import com.civicsense.verification.service.IdentityVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/identity-verifications")
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    @PostMapping(
            value = "/document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<IdentityDocumentUploadResponse> uploadDocument(
            @RequestParam("document") MultipartFile document,
            @RequestParam("documentType") DocumentType documentType
    ) {

        IdentityDocumentUploadResponse response =
                identityVerificationService.uploadDocument(
                        document,
                        documentType
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<IdentityVerificationResponse> confirmIdentity(
            @RequestBody IdentityConfirmationRequest request
    ) {

        IdentityVerificationResponse response =
                identityVerificationService.confirmIdentity(request);

        return ResponseEntity.ok(response);
    }
}
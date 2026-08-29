package com.civicsense.complaint.service.image;

public record ImageUploadResult(
        String imageUrl,
        String storageKey
) {
}
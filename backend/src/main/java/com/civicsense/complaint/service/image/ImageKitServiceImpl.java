package com.civicsense.complaint.service.image;

import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImageKitServiceImpl implements ImageKitService {

    private final ImageKitClient imageKitClient;

    @Override
    public ImageUploadResult uploadImage(
            MultipartFile image
    ) {
        try {
            return uploadImage(image.getBytes(), image.getOriginalFilename());
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to read image file",
                    exception
            );
        }
    }

    @Override
    public ImageUploadResult uploadImage(
            byte[] imageBytes,
            String fileName
    ) {
        try {
            FileUploadParams params =
                    FileUploadParams.builder()
                            .file(imageBytes)
                            .fileName(fileName != null ? fileName : "complaint_image.jpg")
                            .build();

            FileUploadResponse response =
                    imageKitClient.files().upload(params);

            return new ImageUploadResult(
                    response.url().orElseThrow(
                            () -> new RuntimeException("ImageKit did not return image URL")
                    ),
                    response.fileId().orElseThrow(
                            () -> new RuntimeException("ImageKit did not return file ID")
                    )
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to upload image to ImageKit: " + exception.getMessage(),
                    exception
            );
        }
    }
}
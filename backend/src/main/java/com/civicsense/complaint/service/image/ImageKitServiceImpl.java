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

            byte[] imageBytes =
                    image.getBytes();

            FileUploadParams params =
                    FileUploadParams.builder()
                            .file(imageBytes)
                            .fileName(image.getOriginalFilename())
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

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to read image file",
                    exception
            );
        }
    }
}
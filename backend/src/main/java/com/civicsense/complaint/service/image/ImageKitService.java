package com.civicsense.complaint.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageKitService {

    ImageUploadResult uploadImage(MultipartFile image);
}
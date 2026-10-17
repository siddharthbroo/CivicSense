package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.image.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintImageRepository
        extends JpaRepository<ComplaintImage, UUID> {

    Optional<ComplaintImage> findTopByComplaintIdOrderByCreatedAtDesc(UUID complaintId);

    List<ComplaintImage> findByImageHash(String imageHash);

    List<ComplaintImage> findByPerceptualHash(String perceptualHash);
}

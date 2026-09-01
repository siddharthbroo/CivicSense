package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.image.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplaintImageRepository
        extends JpaRepository<ComplaintImage, UUID> {

    Optional<ComplaintImage> findTopByComplaintIdOrderByCreatedAtDesc(UUID complaintId);
}

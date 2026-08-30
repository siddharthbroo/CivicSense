package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.ComplaintAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplaintAnalysisRepository
        extends JpaRepository<ComplaintAnalysis, UUID> {

    Optional<ComplaintAnalysis> findByComplaintId(UUID complaintId);
}
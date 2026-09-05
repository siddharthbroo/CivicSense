package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintAnalysisRepository
        extends JpaRepository<ComplaintAnalysis, UUID> {

    Optional<ComplaintAnalysis> findByComplaintId(UUID complaintId);

    List<ComplaintAnalysis> findByCategoryAndComplaintIdNot(
            ComplaintCategory category,
            UUID complaintId
    );
}
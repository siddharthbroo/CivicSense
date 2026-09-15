package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.risk.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository
        extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByComplaintId(UUID complaintId);
}
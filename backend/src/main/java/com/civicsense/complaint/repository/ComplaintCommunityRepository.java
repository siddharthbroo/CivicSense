package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.community.ComplaintCommunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplaintCommunityRepository extends JpaRepository<ComplaintCommunity, UUID> {

    Optional<ComplaintCommunity> findByComplaintId(UUID complaintId);

    boolean existsByComplaintId(UUID complaintId);
}


package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.community.ComplaintSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintSupportRepository extends JpaRepository<ComplaintSupport, UUID> {

    boolean existsByComplaintIdAndUserId(UUID complaintId, UUID userId);

    Optional<ComplaintSupport> findByComplaintIdAndUserId(UUID complaintId, UUID userId);

    long countByComplaintId(UUID complaintId);

    List<ComplaintSupport> findByComplaintId(UUID complaintId);

    List<ComplaintSupport> findByComplaintIdIn(Collection<UUID> complaintIds);

    long countByComplaintIdIn(Collection<UUID> complaintIds);
}


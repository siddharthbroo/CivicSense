package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.community.ComplaintComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, UUID> {

    Page<ComplaintComment> findByComplaintId(UUID complaintId, Pageable pageable);

    long countByComplaintId(UUID complaintId);
}


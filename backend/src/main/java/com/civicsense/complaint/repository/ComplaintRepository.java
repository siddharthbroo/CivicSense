package com.civicsense.complaint.repository;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import java.util.List;
import com.civicsense.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository
        extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Complaint> findByCategoryAndIdNot(
            String category,
            UUID complaintId
    );
}

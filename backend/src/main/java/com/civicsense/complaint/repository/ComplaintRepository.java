package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintRepository
        extends JpaRepository<Complaint, UUID> {
}
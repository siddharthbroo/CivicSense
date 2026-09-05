package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintClusterRepository
        extends JpaRepository<ComplaintCluster, UUID> {
}
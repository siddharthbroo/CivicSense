package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT a FROM ComplaintAnalysis a
        JOIN a.complaint c
        WHERE a.category = :category
          AND c.id <> :complaintId
          AND c.latitude BETWEEN :minLat AND :maxLat
          AND c.longitude BETWEEN :minLon AND :maxLon
    """)
    List<ComplaintAnalysis> findCandidates(
            @Param("category") ComplaintCategory category,
            @Param("complaintId") UUID complaintId,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon
    );
}
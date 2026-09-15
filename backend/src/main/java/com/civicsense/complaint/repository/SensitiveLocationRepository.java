package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.risk.SensitiveLocation;
import com.civicsense.complaint.entity.risk.SensitiveLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SensitiveLocationRepository
        extends JpaRepository<SensitiveLocation, UUID> {

    Optional<SensitiveLocation> findByNameAndTypeAndLatitudeAndLongitude(
            String name,
            SensitiveLocationType type,
            Double latitude,
            Double longitude
    );

    @Query("""
            SELECT s
            FROM SensitiveLocation s
            WHERE s.latitude BETWEEN :minLatitude AND :maxLatitude
            AND s.longitude BETWEEN :minLongitude AND :maxLongitude
            """)
    List<SensitiveLocation> findNearbyCandidates(
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude
    );
}
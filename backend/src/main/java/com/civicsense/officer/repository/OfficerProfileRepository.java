package com.civicsense.officer.repository;

import com.civicsense.officer.entity.OfficerProfile;
import com.civicsense.officer.entity.OfficerVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfficerProfileRepository extends JpaRepository<OfficerProfile, UUID> {

    Optional<OfficerProfile> findByUserId(UUID userId);

    @Query("SELECT op FROM OfficerProfile op JOIN FETCH op.user WHERE op.user.id = :userId")
    Optional<OfficerProfile> findByUserIdWithUser(@Param("userId") UUID userId);

    Optional<OfficerProfile> findByMunicipalityAndEmployeeCode(String municipality, String employeeCode);

    boolean existsByUserId(UUID userId);

    boolean existsByMunicipalityAndEmployeeCode(String municipality, String employeeCode);

    List<OfficerProfile> findByMunicipality(String municipality);

    List<OfficerProfile> findByVerificationStatus(OfficerVerificationStatus status);

    List<OfficerProfile> findByMunicipalityAndVerificationStatus(String municipality, OfficerVerificationStatus status);

    List<OfficerProfile> findByDepartment(String department);
}


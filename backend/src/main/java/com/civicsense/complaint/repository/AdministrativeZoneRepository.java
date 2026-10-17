package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.geo.AdministrativeZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdministrativeZoneRepository extends JpaRepository<AdministrativeZone, UUID> {

    Optional<AdministrativeZone> findByCode(String code);

    Optional<AdministrativeZone> findByName(String name);

    Optional<AdministrativeZone> findByMunicipalityAndCode(String municipality, String code);

    List<AdministrativeZone> findByMunicipality(String municipality);

    List<AdministrativeZone> findByActiveTrue();

    List<AdministrativeZone> findByMunicipalityAndActiveTrue(String municipality);

    boolean existsByMunicipalityAndCode(String municipality, String code);
}
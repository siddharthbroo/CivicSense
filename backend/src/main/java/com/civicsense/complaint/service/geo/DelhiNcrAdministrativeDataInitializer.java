package com.civicsense.complaint.service.geo;

import com.civicsense.complaint.entity.geo.AdministrativeWard;
import com.civicsense.complaint.entity.geo.AdministrativeZone;
import com.civicsense.complaint.entity.geo.BoundaryProvenance;
import com.civicsense.complaint.repository.AdministrativeWardRepository;
import com.civicsense.complaint.repository.AdministrativeZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes baseline Delhi NCR Administrative Zones and Wards if the database is empty.
 * Uses provisional bounding boxes representing MCD administrative areas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelhiNcrAdministrativeDataInitializer implements CommandLineRunner {

    private static final String MCD_MUNICIPALITY = "Municipal Corporation of Delhi (MCD)";
    private static final String PROVISIONAL_SOURCE = "Provisional NCT Delhi baseline bounding boxes";

    private final AdministrativeZoneRepository zoneRepository;
    private final AdministrativeWardRepository wardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (zoneRepository.count() > 0) {
            return;
        }

        log.info("Initializing baseline Delhi NCR Zones and Wards with PROVISIONAL_BOUNDS provenance...");

        // 1. South Zone
        AdministrativeZone southZone = createZone("South Delhi Zone", "DL-SOUTH");
        createWard(southZone, "Hauz Khas", "WARD-142-S",
                28.5200, 28.5700, 77.1800, 77.2300, 28.5494, 77.2001);
        createWard(southZone, "Saket & Malviya Nagar", "WARD-143-S",
                28.5000, 28.5400, 77.2000, 77.2400, 28.5244, 77.2167);
        createWard(southZone, "Greater Kailash", "WARD-144-S",
                28.5300, 28.5600, 77.2300, 77.2600, 28.5482, 77.2426);

        // 2. Central Zone
        AdministrativeZone centralZone = createZone("Central Delhi Zone", "DL-CENTRAL");
        createWard(centralZone, "Connaught Place & Civil Lines", "WARD-055-C",
                28.6100, 28.6600, 77.2000, 77.2400, 28.6315, 77.2167);
        createWard(centralZone, "Karol Bagh & Rajendra Nagar", "WARD-056-C",
                28.6300, 28.6700, 77.1700, 77.2100, 28.6517, 77.1906);

        // 3. North Zone (Rohini)
        AdministrativeZone northZone = createZone("North & Rohini Zone", "DL-NORTH");
        createWard(northZone, "Rohini Sector 1-15", "WARD-021-N",
                28.6900, 28.7400, 77.0800, 77.1400, 28.7142, 77.1126);
        createWard(northZone, "Pitampura", "WARD-022-N",
                28.6800, 28.7200, 77.1200, 77.1600, 28.6989, 77.1384);

        // 4. East Zone
        AdministrativeZone eastZone = createZone("East Delhi Zone", "DL-EAST");
        createWard(eastZone, "Mayur Vihar & Patparganj", "WARD-088-E",
                28.5900, 28.6300, 77.2800, 77.3200, 28.6083, 77.2989);
        createWard(eastZone, "Laxmi Nagar & Preet Vihar", "WARD-089-E",
                28.6200, 28.6600, 77.2700, 77.3100, 28.6333, 77.2833);

        // 5. West Zone
        AdministrativeZone westZone = createZone("West Delhi Zone", "DL-WEST");
        createWard(westZone, "Janakpuri & Tilak Nagar", "WARD-105-W",
                28.6100, 28.6500, 77.0700, 77.1100, 28.6295, 77.0867);
        createWard(westZone, "Dwarka Sectors", "WARD-106-W",
                28.5600, 28.6100, 77.0200, 77.0800, 28.5823, 77.0500);

        log.info("Delhi NCR Zones and Wards successfully initialized with explicit provenance.");
    }

    private AdministrativeZone createZone(String name, String code) {
        AdministrativeZone zone = new AdministrativeZone(name, code, MCD_MUNICIPALITY, "Delhi NCR");
        zone.setDataSource(PROVISIONAL_SOURCE);
        return zoneRepository.save(zone);
    }

    private void createWard(
            AdministrativeZone zone,
            String name,
            String wardNumber,
            Double minLat, Double maxLat,
            Double minLon, Double maxLon,
            Double centerLat, Double centerLon
    ) {
        AdministrativeWard ward = new AdministrativeWard(
                zone, name, wardNumber, MCD_MUNICIPALITY, BoundaryProvenance.PROVISIONAL_BOUNDS,
                minLat, maxLat, minLon, maxLon, centerLat, centerLon
        );
        ward.setDataSource(PROVISIONAL_SOURCE);
        wardRepository.save(ward);
    }
}
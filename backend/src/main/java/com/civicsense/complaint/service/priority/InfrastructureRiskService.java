package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import org.springframework.stereotype.Service;

@Service
public class InfrastructureRiskService {

    public double calculateInfrastructureRisk(
            Complaint complaint
    ) {

        if (complaint.getCategory() == null
                || complaint.getCategory().isBlank()) {
            return 0.0;
        }

        ComplaintCategory category;

        try {
            category = ComplaintCategory.valueOf(
                    complaint.getCategory()
            );
        } catch (IllegalArgumentException e) {
            return 0.0;
        }

        return switch (category) {

            case DRAINAGE -> 85.0;

            case SEWAGE -> 85.0;

            case TRAFFIC_SIGNAL -> 80.0;

            case ROAD_DAMAGE -> 75.0;

            case WATER_LEAKAGE -> 70.0;

            case STREET_LIGHT -> 60.0;

            case ILLEGAL_DUMPING -> 60.0;

            case AIR_POLLUTION -> 65.0;

            case GARBAGE -> 50.0;

            case PUBLIC_TOILET -> 45.0;

            case ENCROACHMENT -> 45.0;

            case STRAY_ANIMAL -> 40.0;

            case TREE_RELATED -> 40.0;

            case PARK_MAINTENANCE -> 30.0;

            case NOISE_POLLUTION -> 30.0;

            case OTHER -> 20.0;
        };
    }
}
package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.risk.SensitiveLocationType;

public record GeoapifyPlace(
        String name,
        SensitiveLocationType type,
        double latitude,
        double longitude
) {
}
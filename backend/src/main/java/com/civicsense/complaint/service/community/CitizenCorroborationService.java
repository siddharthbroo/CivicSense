package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;

import java.util.UUID;

public interface CitizenCorroborationService {

    CorroborationIntelligence getCorroboration(UUID complaintId);

    CorroborationIntelligence analyzeCorroboration(UUID complaintId);

    CorroborationIntelligence analyzeComplaintCorroboration(Complaint complaint);

    CorroborationIntelligence analyzeClusterCorroboration(UUID clusterId);
}


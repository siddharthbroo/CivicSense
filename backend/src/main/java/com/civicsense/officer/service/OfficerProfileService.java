package com.civicsense.officer.service;

import com.civicsense.officer.dto.OfficerProfileResponse;
import com.civicsense.officer.dto.ProvisionOfficerRequest;
import com.civicsense.officer.dto.UpdateOfficerVerificationRequest;
import com.civicsense.officer.entity.OfficerVerificationStatus;

import java.util.List;
import java.util.UUID;

public interface OfficerProfileService {

    OfficerProfileResponse provisionOfficer(ProvisionOfficerRequest request);

    OfficerProfileResponse updateVerificationStatus(UUID officerProfileId, UpdateOfficerVerificationRequest request);

    OfficerProfileResponse getOfficerProfileById(UUID id);

    OfficerProfileResponse getOfficerProfileByUserId(UUID userId);

    List<OfficerProfileResponse> getOfficers(String municipality, OfficerVerificationStatus status);
}


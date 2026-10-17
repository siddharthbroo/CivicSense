package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CommunityComplaintResponse;
import com.civicsense.complaint.dto.community.ComplaintCommunityResponse;
import com.civicsense.complaint.entity.ComplaintStatus;

import java.util.List;
import java.util.UUID;

public interface ComplaintCommunityService {

    ComplaintCommunityResponse supportComplaint(UUID complaintId, UUID userId);

    ComplaintCommunityResponse removeSupport(UUID complaintId, UUID userId);

    ComplaintCommunityResponse getCommunityRepresentation(UUID complaintId, UUID currentUserId);

    CommunityComplaintResponse getCommunityComplaint(UUID complaintId, UUID currentUserId);

    List<CommunityComplaintResponse> getCommunityFeed(ComplaintStatus status, String sort, UUID currentUserId);

    List<CommunityComplaintResponse> getNearbyCommunityComplaints(Double latitude, Double longitude, Double radiusKm, UUID currentUserId);

    boolean hasUserSupported(UUID complaintId, UUID userId);

    long getSupportCount(UUID complaintId);
}

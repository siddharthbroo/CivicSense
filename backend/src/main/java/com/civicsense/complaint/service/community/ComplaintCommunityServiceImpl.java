package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CommunityComplaintResponse;
import com.civicsense.complaint.dto.community.ComplaintCommunityResponse;
import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.entity.community.ComplaintCommunity;
import com.civicsense.complaint.entity.community.ComplaintSupport;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintCommunityRepository;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.complaint.entity.risk.RiskAssessment;
import com.civicsense.complaint.repository.ComplaintSupportRepository;
import com.civicsense.complaint.repository.RiskAssessmentRepository;
import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintCommunityServiceImpl implements ComplaintCommunityService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintSupportRepository complaintSupportRepository;
    private final ComplaintCommunityRepository complaintCommunityRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final CitizenCorroborationService citizenCorroborationService;

    @Override
    @Transactional
    public ComplaintCommunityResponse supportComplaint(UUID complaintId, UUID userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userId)) {
            throw new IllegalStateException("User has already supported this complaint");
        }

        ComplaintSupport support = new ComplaintSupport(complaint, user);
        complaintSupportRepository.save(support);

        ComplaintCommunity community = complaintCommunityRepository.findByComplaintId(complaintId)
                .orElseGet(() -> new ComplaintCommunity(complaint));

        long totalSupports = complaintSupportRepository.countByComplaintId(complaintId);
        community.setSupportCount(totalSupports);
        community = complaintCommunityRepository.save(community);

        return new ComplaintCommunityResponse(
                complaintId,
                community.getSupportCount(),
                true,
                community.getCreatedAt(),
                community.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public ComplaintCommunityResponse removeSupport(UUID complaintId, UUID userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ComplaintSupport support = complaintSupportRepository.findByComplaintIdAndUserId(complaintId, userId)
                .orElseThrow(() -> new IllegalStateException("User has not supported this complaint"));

        complaintSupportRepository.delete(support);

        ComplaintCommunity community = complaintCommunityRepository.findByComplaintId(complaintId)
                .orElseGet(() -> new ComplaintCommunity(complaint));

        long totalSupports = complaintSupportRepository.countByComplaintId(complaintId);
        community.setSupportCount(totalSupports);
        community = complaintCommunityRepository.save(community);

        return new ComplaintCommunityResponse(
                complaintId,
                community.getSupportCount(),
                false,
                community.getCreatedAt(),
                community.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintCommunityResponse getCommunityRepresentation(UUID complaintId, UUID currentUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        ComplaintCommunity community = complaintCommunityRepository.findByComplaintId(complaintId)
                .orElse(null);

        long supportCount = (community != null)
                ? community.getSupportCount()
                : complaintSupportRepository.countByComplaintId(complaintId);

        LocalDateTime createdAt = (community != null && community.getCreatedAt() != null)
                ? community.getCreatedAt()
                : complaint.getCreatedAt();

        LocalDateTime updatedAt = (community != null && community.getUpdatedAt() != null)
                ? community.getUpdatedAt()
                : complaint.getUpdatedAt();

        boolean supportedByCurrentUser = false;
        if (currentUserId != null) {
            supportedByCurrentUser = complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, currentUserId);
        }

        return new ComplaintCommunityResponse(
                complaintId,
                supportCount,
                supportedByCurrentUser,
                createdAt,
                updatedAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityComplaintResponse getCommunityComplaint(UUID complaintId, UUID currentUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        return toCommunityComplaintResponse(complaint, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityComplaintResponse> getCommunityFeed(ComplaintStatus status, String sort, UUID currentUserId) {
        List<Complaint> complaints = complaintRepository.findAll();

        List<CommunityComplaintResponse> responses = complaints.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .map(c -> toCommunityComplaintResponse(c, currentUserId))
                .collect(Collectors.toCollection(ArrayList::new));

        if ("MOST_SUPPORTED".equalsIgnoreCase(sort)) {
            responses.sort(Comparator.comparingLong(CommunityComplaintResponse::supportCount).reversed());
        } else {
            // Default sort: RECENT (newest first)
            responses.sort(Comparator.comparing(
                    CommunityComplaintResponse::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserSupported(UUID complaintId, UUID userId) {
        if (complaintId == null || userId == null) {
            return false;
        }
        return complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getSupportCount(UUID complaintId) {
        if (complaintId == null) {
            return 0;
        }
        return complaintCommunityRepository.findByComplaintId(complaintId)
                .map(ComplaintCommunity::getSupportCount)
                .orElseGet(() -> complaintSupportRepository.countByComplaintId(complaintId));
    }

    private CommunityComplaintResponse toCommunityComplaintResponse(Complaint complaint, UUID currentUserId) {
        String imageUrl = complaintImageRepository.findTopByComplaintIdOrderByCreatedAtDesc(complaint.getId())
                .map(ComplaintImage::getImageUrl)
                .orElse(null);

        ComplaintPriority priority = riskAssessmentRepository.findByComplaintId(complaint.getId())
                .map(RiskAssessment::getPriority)
                .orElse(null);

        CorroborationIntelligence corroboration = citizenCorroborationService.analyzeComplaintCorroboration(complaint);

        boolean supportedByCurrentUser = false;
        if (currentUserId != null) {
            supportedByCurrentUser = complaintSupportRepository.existsByComplaintIdAndUserId(complaint.getId(), currentUserId);
        }

        return new CommunityComplaintResponse(
                complaint.getId(),
                imageUrl,
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getCategory(),
                complaint.getAddress(),
                complaint.getLatitude(),
                complaint.getLongitude(),
                priority,
                complaint.getStatus(),
                corroboration.supportCount(),
                0L, // commentCount
                corroboration.totalReports(),
                corroboration.uniqueCitizens(),
                corroboration.recurring(),
                corroboration.recurrenceCount(),
                corroboration.firstReportedAt(),
                corroboration.lastReportedAt(),
                supportedByCurrentUser,
                complaint.getCreatedAt(),
                complaint.getUpdatedAt()
        );
    }
}

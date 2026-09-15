package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import com.civicsense.complaint.entity.community.ComplaintSupport;
import com.civicsense.complaint.repository.ComplaintClusterRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.repository.ComplaintSupportRepository;
import com.civicsense.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CitizenCorroborationServiceImpl implements CitizenCorroborationService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintClusterRepository complaintClusterRepository;
    private final ComplaintSupportRepository complaintSupportRepository;

    @Override
    @Transactional(readOnly = true)
    public CorroborationIntelligence getCorroboration(UUID complaintId) {
        return analyzeCorroboration(complaintId);
    }

    @Override
    @Transactional(readOnly = true)
    public CorroborationIntelligence analyzeCorroboration(UUID complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        return analyzeComplaintCorroboration(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public CorroborationIntelligence analyzeComplaintCorroboration(Complaint complaint) {
        if (complaint == null) {
            throw new IllegalArgumentException("Complaint cannot be null");
        }

        ComplaintCluster cluster = complaint.getCluster();
        if (cluster == null) {
            // Unclustered single complaint
            int totalReports = 1;
            int uniqueCitizens = 1;

            List<ComplaintSupport> supports = complaintSupportRepository.findByComplaintId(complaint.getId());
            if (supports == null || supports.isEmpty()) {
                supports = complaintSupportRepository.findByComplaintIdIn(List.of(complaint.getId()));
            }

            long supportCount;
            long uniqueSupporters;
            if (supports != null && !supports.isEmpty()) {
                supportCount = supports.size();
                uniqueSupporters = supports.stream()
                        .map(s -> s.getUser() != null ? s.getUser().getId() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();
            } else {
                supportCount = complaintSupportRepository.countByComplaintId(complaint.getId());
                uniqueSupporters = supportCount;
            }

            boolean recurring = false;
            int recurrenceCount = 0;
            LocalDateTime firstReportedAt = complaint.getCreatedAt();
            LocalDateTime lastReportedAt = complaint.getCreatedAt();
            double score = calculateIsolatedScore(uniqueCitizens, totalReports, uniqueSupporters);

            return new CorroborationIntelligence(
                    complaint.getId(),
                    null,
                    totalReports,
                    uniqueCitizens,
                    supportCount,
                    uniqueSupporters,
                    recurring,
                    recurrenceCount,
                    firstReportedAt,
                    lastReportedAt,
                    score
            );
        }

        List<Complaint> clusterComplaints = complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId());
        if (clusterComplaints.isEmpty()) {
            clusterComplaints = List.of(complaint);
        }

        return buildClusterIntelligence(complaint.getId(), cluster.getId(), clusterComplaints);
    }

    @Override
    @Transactional(readOnly = true)
    public CorroborationIntelligence analyzeClusterCorroboration(UUID clusterId) {
        ComplaintCluster cluster = complaintClusterRepository.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("ComplaintCluster not found with id: " + clusterId));

        List<Complaint> clusterComplaints = complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId());
        UUID targetComplaintId = clusterComplaints.isEmpty() ? null : clusterComplaints.get(0).getId();

        return buildClusterIntelligence(targetComplaintId, cluster.getId(), clusterComplaints);
    }

    private CorroborationIntelligence buildClusterIntelligence(
            UUID targetComplaintId,
            UUID clusterId,
            List<Complaint> complaints
    ) {
        int totalReports = complaints.size();
        int uniqueCitizens = (int) complaints.stream()
                .map(c -> c.getUser() != null ? c.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        LocalDateTime firstReportedAt = complaints.stream()
                .map(Complaint::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastReportedAt = complaints.stream()
                .map(Complaint::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        boolean recurring = totalReports > 1;
        int recurrenceCount = Math.max(0, totalReports - 1);

        List<UUID> complaintIds = complaints.stream()
                .map(Complaint::getId)
                .filter(Objects::nonNull)
                .toList();

        List<ComplaintSupport> supports = complaintIds.isEmpty()
                ? List.of()
                : complaintSupportRepository.findByComplaintIdIn(complaintIds);

        long supportCount = supports.size();
        long uniqueSupporters = supports.stream()
                .map(s -> s.getUser() != null ? s.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        double score = calculateIsolatedScore(uniqueCitizens, totalReports, uniqueSupporters);

        return new CorroborationIntelligence(
                targetComplaintId,
                clusterId,
                totalReports,
                uniqueCitizens,
                supportCount,
                uniqueSupporters,
                recurring,
                recurrenceCount,
                firstReportedAt,
                lastReportedAt,
                score
        );
    }

    private double calculateIsolatedScore(int uniqueCitizens, int totalReports, long uniqueSupporters) {
        // 1. Independent citizens (primary corroboration signal, weight up to 75 points)
        // 1 citizen = 25, 2 citizens = 50, 3+ citizens = 75
        double citizenScore = Math.min(uniqueCitizens * 25.0, 75.0);

        // 2. Repeat reports from citizens (secondary corroboration signal, weight up to 10 points)
        int repeatReports = Math.max(0, totalReports - uniqueCitizens);
        double repeatScore = Math.min(repeatReports * 5.0, 10.0);

        // 3. Community supporters clicking "I'm affected too" (weight up to 15 points)
        double supportScore = Math.min(uniqueSupporters * 5.0, 15.0);

        double total = citizenScore + repeatScore + supportScore;
        return Math.min(Math.round(total * 100.0) / 100.0, 100.0);
    }
}


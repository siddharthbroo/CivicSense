package com.civicsense.complaint.service;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import com.civicsense.complaint.repository.ComplaintClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComplaintClusterService {

    private final ComplaintClusterRepository complaintClusterRepository;

    public ComplaintCluster assignToExistingCluster(
            Complaint complaint,
            Complaint matchedComplaint
    ) {
        ComplaintCluster cluster = matchedComplaint.getCluster();

        if (cluster == null) {
            throw new IllegalStateException(
                    "Matched complaint is not assigned to any cluster"
            );
        }

        complaint.setCluster(cluster);

        cluster.setComplaintCount(
                cluster.getComplaintCount() + 1
        );

        complaintClusterRepository.save(cluster);

        return cluster;
    }

    public ComplaintCluster createNewCluster(
            Complaint complaint,
            ComplaintCategory category
    ) {
        ComplaintCluster cluster = new ComplaintCluster();

        cluster.setCategory(category);
        cluster.setComplaintCount(1);
        cluster.setLatitude(complaint.getLatitude());
        cluster.setLongitude(complaint.getLongitude());

        ComplaintCluster savedCluster =
                complaintClusterRepository.save(cluster);

        complaint.setCluster(savedCluster);

        return savedCluster;
    }
}
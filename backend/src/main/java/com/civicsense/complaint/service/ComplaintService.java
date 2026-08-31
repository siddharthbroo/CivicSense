package com.civicsense.complaint.service;

import com.civicsense.complaint.dto.ComplaintResponse;
import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ComplaintService {

    CreateComplaintResponse createComplaint(
            UUID userId,
            CreateComplaintRequest request,
            MultipartFile image
    );

    List<ComplaintResponse> getMyComplaints(UUID userId);
}
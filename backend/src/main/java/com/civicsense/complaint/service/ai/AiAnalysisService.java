package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.image.ComplaintImage;

public interface AiAnalysisService {

    AiAnalysisResult analyze(
            Complaint complaint,
            ComplaintImage complaintImage
    );
}
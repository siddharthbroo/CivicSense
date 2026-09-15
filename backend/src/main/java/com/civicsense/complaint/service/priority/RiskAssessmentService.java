package com.civicsense.complaint.service.priority;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.risk.RiskAssessment;

public interface RiskAssessmentService {

    RiskAssessment assess(
            Complaint complaint,
            ComplaintAnalysis analysis
    );
}
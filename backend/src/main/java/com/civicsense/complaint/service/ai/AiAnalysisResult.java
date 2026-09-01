package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.classification.ComplaintCategory;
import com.civicsense.complaint.entity.classification.ComplaintDepartment;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.ImageAuthenticity;

public record AiAnalysisResult(

        String language,

        ComplaintCategory category,

        ComplaintSeverity severity,

        String summary,

        ComplaintDepartment department,

        double confidence,

        ImageAuthenticity imageAuthenticity,

        DescriptionImageConsistency descriptionImageConsistency

) {
}
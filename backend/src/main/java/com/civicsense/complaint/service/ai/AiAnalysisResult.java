package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.ComplaintCategory;
import com.civicsense.complaint.entity.ComplaintDepartment;
import com.civicsense.complaint.entity.ComplaintSeverity;
import com.civicsense.complaint.entity.DescriptionImageConsistency;
import com.civicsense.complaint.entity.ImageAuthenticity;

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
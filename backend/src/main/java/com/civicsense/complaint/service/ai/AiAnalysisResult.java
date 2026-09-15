package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.classification.ComplaintCategory;
import com.civicsense.complaint.entity.classification.ComplaintDepartment;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.ImageAuthenticity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisResult(

                String language,

                String title,

                ComplaintCategory category,

                ComplaintSeverity severity,

                String summary,

                ComplaintDepartment department,

                double confidence,

                ImageAuthenticity imageAuthenticity,

                DescriptionImageConsistency descriptionImageConsistency,

                ComplaintSeverity safetyRisk,

                Boolean immediateHazard,

                ComplaintSeverity publicImpact,

                String potentialHarm,

                ComplaintSeverity recommendedUrgency

) {
}
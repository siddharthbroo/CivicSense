package com.civicsense.complaint.service.ai;

public record AiAnalysisResult(

        String language,

        String category,

        String severity,

        String summary,

        String department,

        double confidence,

        String imageAuthenticity,

        String descriptionImageConsistency
) {
}
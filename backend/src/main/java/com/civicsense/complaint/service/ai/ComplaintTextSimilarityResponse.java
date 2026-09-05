package com.civicsense.complaint.service.ai;

public record ComplaintTextSimilarityResponse(
        double title_similarity,
        double description_similarity,
        double summary_similarity,
        boolean category_match
) {
}
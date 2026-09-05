package com.civicsense.com.civicsense.complaint.service;

import com.civicsense.complaint.service.ComplaintDuplicateScoringService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComplaintDuplicateScoringServiceTest {

    private final ComplaintDuplicateScoringService scoringService =
            new ComplaintDuplicateScoringService();

    @Test
    void shouldMarkAsDuplicateWhenAllSignalsAreStrong() {

        double score = scoringService.calculateCombinedScore(
                1.0,   // title
                1.0,   // description
                1.0,   // summary
                1.0,   // image
                1.0,   // address
                0.0    // distance
        );

        ComplaintDuplicateScoringService.DuplicateDecision decision =
                scoringService.decide(score, true);

        assertEquals(1.0, score);
        assertEquals(
                ComplaintDuplicateScoringService.DuplicateDecision.DUPLICATE,
                decision
        );
    }

    @Test
    void shouldMarkAsPotentialDuplicateWhenSimilarityIsModerate() {

        double score = scoringService.calculateCombinedScore(
                0.7,
                0.7,
                0.7,
                0.7,
                0.7,
                25.0
        );

        ComplaintDuplicateScoringService.DuplicateDecision decision =
                scoringService.decide(score, true);

        assertEquals(
                ComplaintDuplicateScoringService.DuplicateDecision.POTENTIAL_DUPLICATE,
                decision
        );
    }

    @Test
    void shouldKeepComplaintsSeparateWhenCategoryDoesNotMatch() {

        double score = scoringService.calculateCombinedScore(
                1.0,
                1.0,
                1.0,
                1.0,
                1.0,
                0.0
        );

        ComplaintDuplicateScoringService.DuplicateDecision decision =
                scoringService.decide(score, false);

        assertEquals(
                ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                decision
        );
    }

    @Test
    void shouldKeepComplaintsSeparateWhenSimilarityIsLow() {

        double score = scoringService.calculateCombinedScore(
                0.2,
                0.2,
                0.2,
                0.2,
                0.2,
                100.0
        );

        ComplaintDuplicateScoringService.DuplicateDecision decision =
                scoringService.decide(score, true);

        assertEquals(
                ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                decision
        );
    }
}
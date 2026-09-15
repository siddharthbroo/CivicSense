package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import com.civicsense.complaint.entity.community.ComplaintSupport;
import com.civicsense.complaint.repository.ComplaintClusterRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.repository.ComplaintSupportRepository;
import com.civicsense.complaint.service.complaintCluster.ComplaintDuplicateScoringService;
import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenCorroborationServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintClusterRepository complaintClusterRepository;

    @Mock
    private ComplaintSupportRepository complaintSupportRepository;

    @InjectMocks
    private CitizenCorroborationServiceImpl corroborationService;

    private User citizenA;
    private User citizenB;
    private User citizenC;
    private User citizenD;
    private ComplaintCluster cluster;

    @BeforeEach
    void setUp() {
        citizenA = new User();
        citizenA.setId(UUID.randomUUID());
        citizenA.setName("Citizen A");

        citizenB = new User();
        citizenB.setId(UUID.randomUUID());
        citizenB.setName("Citizen B");

        citizenC = new User();
        citizenC.setId(UUID.randomUUID());
        citizenC.setName("Citizen C");

        citizenD = new User();
        citizenD.setId(UUID.randomUUID());
        citizenD.setName("Citizen D");

        cluster = new ComplaintCluster();
        cluster.setId(UUID.randomUUID());
        cluster.setComplaintCount(1);
    }

    private Complaint createComplaint(User user, ComplaintCluster cluster, LocalDateTime createdAt) {
        Complaint c = new Complaint();
        c.setId(UUID.randomUUID());
        c.setUser(user);
        c.setCluster(cluster);
        c.setCreatedAt(createdAt);
        return c;
    }

    @Test
    @DisplayName("1. Single unclustered complaint")
    void testSingleUnclusteredComplaint() {
        LocalDateTime now = LocalDateTime.now();
        Complaint complaint = createComplaint(citizenA, null, now);

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintSupportRepository.findByComplaintId(complaint.getId())).thenReturn(Collections.emptyList());
        when(complaintSupportRepository.countByComplaintId(complaint.getId())).thenReturn(0L);

        CorroborationIntelligence result = corroborationService.getCorroboration(complaint.getId());

        assertNotNull(result);
        assertEquals(complaint.getId(), result.complaintId());
        assertNull(result.clusterId(), "ClusterId must be null for unclustered complaint");
        assertEquals(1, result.totalReports());
        assertEquals(1, result.uniqueCitizens());
        assertFalse(result.recurring());
        assertEquals(0, result.recurrenceCount());
        assertEquals(0L, result.supportCount());
        assertEquals(0L, result.uniqueSupporters());
        assertEquals(now, result.firstReportedAt());
        assertEquals(now, result.lastReportedAt());
        assertEquals(25.0, result.corroborationScore());
    }

    @Test
    @DisplayName("2. Cluster with multiple independent complaints")
    void testClusterWithMultipleIndependentComplaints() {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> complaints = List.of(
                createComplaint(citizenA, cluster, now.minusHours(4)),
                createComplaint(citizenB, cluster, now.minusHours(3)),
                createComplaint(citizenC, cluster, now.minusHours(2)),
                createComplaint(citizenD, cluster, now.minusHours(1))
        );
        cluster.setComplaintCount(4);

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertNotNull(result);
        assertEquals(4, result.totalReports(), "Total reports should match complaints count");
        assertEquals(4, result.uniqueCitizens(), "4 independent citizens should yield uniqueCitizens = 4");
        assertTrue(result.recurring());
        assertEquals(3, result.recurrenceCount());
        assertEquals(75.0, result.corroborationScore());
    }

    @Test
    @DisplayName("3. Same citizen submitting multiple complaints")
    void testSameCitizenSubmittingMultipleComplaints() {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(3);
        List<Complaint> complaints = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            complaints.add(createComplaint(citizenA, cluster, baseTime.plusHours(i)));
        }
        cluster.setComplaintCount(5);

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertNotNull(result);
        assertEquals(5, result.totalReports(), "Total reports should be 5");
        assertEquals(1, result.uniqueCitizens(), "Unique citizens must be 1 when same citizen submits multiple");
        assertTrue(result.recurring());
        assertEquals(4, result.recurrenceCount());
        // Score: 1 citizen (25) + 4 repeat reports (min(4*5, 10) = 10) = 35.0
        assertEquals(35.0, result.corroborationScore());
    }

    @Test
    @DisplayName("4. Support count")
    void testSupportCount() {
        LocalDateTime now = LocalDateTime.now();
        Complaint complaint = createComplaint(citizenA, cluster, now);

        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());
        User supporter2 = new User();
        supporter2.setId(UUID.randomUUID());

        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(complaint, supporter1),
                new ComplaintSupport(complaint, supporter2)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(List.of(complaint));
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(supports);

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaint);

        assertNotNull(result);
        assertEquals(2L, result.supportCount());
        assertEquals(2L, result.uniqueSupporters());
    }

    @Test
    @DisplayName("5. Unique supporters")
    void testUniqueSupportersDeDuplication() {
        LocalDateTime now = LocalDateTime.now();
        Complaint complaint1 = createComplaint(citizenA, cluster, now);
        Complaint complaint2 = createComplaint(citizenB, cluster, now.plusMinutes(10));

        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());

        // Same user supports both complaints in the same cluster
        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(complaint1, supporter1),
                new ComplaintSupport(complaint2, supporter1)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(List.of(complaint1, complaint2));
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(supports);

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaint1);

        assertEquals(2L, result.supportCount(), "Total supports across cluster should be 2");
        assertEquals(1L, result.uniqueSupporters(), "Unique supporters must de-duplicate same user to 1");
    }

    @Test
    @DisplayName("6. Recurring complaint")
    void testRecurringComplaint() {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> complaints = List.of(
                createComplaint(citizenA, cluster, now.minusHours(2)),
                createComplaint(citizenB, cluster, now.minusHours(1)),
                createComplaint(citizenC, cluster, now)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertTrue(result.recurring());
        assertEquals(2, result.recurrenceCount());
        assertEquals(3, result.totalReports());
    }

    @Test
    @DisplayName("7. Non-recurring complaint")
    void testNonRecurringComplaint() {
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> complaints = List.of(createComplaint(citizenA, cluster, now));

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertFalse(result.recurring());
        assertEquals(0, result.recurrenceCount());
        assertEquals(1, result.totalReports());
    }

    @Test
    @DisplayName("8. First and last reported timestamps")
    void testFirstAndLastReportedTimestamps() {
        LocalDateTime firstTime = LocalDateTime.of(2026, 9, 1, 8, 30);
        LocalDateTime middleTime = LocalDateTime.of(2026, 9, 5, 12, 0);
        LocalDateTime lastTime = LocalDateTime.of(2026, 9, 12, 17, 45);

        List<Complaint> complaints = List.of(
                createComplaint(citizenA, cluster, middleTime),
                createComplaint(citizenB, cluster, firstTime),
                createComplaint(citizenC, cluster, lastTime)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertEquals(firstTime, result.firstReportedAt());
        assertEquals(lastTime, result.lastReportedAt());
    }

    @Test
    @DisplayName("9. Corroboration score calculation")
    void testCorroborationScoreCalculation() {
        // 2 independent citizens = 50.0
        // 1 repeat report (3 total - 2 unique) = 5.0
        // 2 unique supporters = 10.0
        // Total score = 65.0
        LocalDateTime now = LocalDateTime.now();
        List<Complaint> complaints = List.of(
                createComplaint(citizenA, cluster, now.minusHours(3)),
                createComplaint(citizenB, cluster, now.minusHours(2)),
                createComplaint(citizenA, cluster, now.minusHours(1))
        );

        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());
        User supporter2 = new User();
        supporter2.setId(UUID.randomUUID());

        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(complaints.get(0), supporter1),
                new ComplaintSupport(complaints.get(0), supporter2)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(supports);

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertEquals(3, result.totalReports());
        assertEquals(2, result.uniqueCitizens());
        assertEquals(2L, result.supportCount());
        assertEquals(2L, result.uniqueSupporters());
        assertEquals(65.0, result.corroborationScore());
    }

    @Test
    @DisplayName("10. Score caps")
    void testScoreCaps() {
        // Test caps:
        // 5 unique citizens -> min(5 * 25, 75) = 75 (capped)
        // 4 repeat reports -> min(4 * 5, 10) = 10 (capped)
        // 4 unique supporters -> min(4 * 5, 15) = 15 (capped)
        // Total = 75 + 10 + 15 = 100.0 (bounded at 100)
        LocalDateTime now = LocalDateTime.now();
        User u1 = new User(); u1.setId(UUID.randomUUID());
        User u2 = new User(); u2.setId(UUID.randomUUID());
        User u3 = new User(); u3.setId(UUID.randomUUID());
        User u4 = new User(); u4.setId(UUID.randomUUID());
        User u5 = new User(); u5.setId(UUID.randomUUID());

        List<Complaint> complaints = List.of(
                createComplaint(u1, cluster, now.minusHours(9)),
                createComplaint(u2, cluster, now.minusHours(8)),
                createComplaint(u3, cluster, now.minusHours(7)),
                createComplaint(u4, cluster, now.minusHours(6)),
                createComplaint(u5, cluster, now.minusHours(5)),
                createComplaint(u1, cluster, now.minusHours(4)),
                createComplaint(u1, cluster, now.minusHours(3)),
                createComplaint(u2, cluster, now.minusHours(2)),
                createComplaint(u2, cluster, now.minusHours(1))
        ); // total = 9, unique = 5, repeat = 4

        User s1 = new User(); s1.setId(UUID.randomUUID());
        User s2 = new User(); s2.setId(UUID.randomUUID());
        User s3 = new User(); s3.setId(UUID.randomUUID());
        User s4 = new User(); s4.setId(UUID.randomUUID());

        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(complaints.get(0), s1),
                new ComplaintSupport(complaints.get(0), s2),
                new ComplaintSupport(complaints.get(0), s3),
                new ComplaintSupport(complaints.get(0), s4)
        );

        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(supports);

        CorroborationIntelligence result = corroborationService.analyzeComplaintCorroboration(complaints.get(0));

        assertEquals(9, result.totalReports());
        assertEquals(5, result.uniqueCitizens());
        assertEquals(4L, result.uniqueSupporters());
        assertEquals(100.0, result.corroborationScore());
    }

    @Test
    @DisplayName("11. Complaint not found")
    void testComplaintNotFoundThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(complaintRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> corroborationService.getCorroboration(nonExistentId));
    }

    @Test
    @DisplayName("12. Unclustered complaint with supports")
    void testUnclusteredComplaintWithSupports() {
        LocalDateTime now = LocalDateTime.now();
        Complaint complaint = createComplaint(citizenA, null, now);

        User supporter1 = new User();
        supporter1.setId(UUID.randomUUID());
        User supporter2 = new User();
        supporter2.setId(UUID.randomUUID());

        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(complaint, supporter1),
                new ComplaintSupport(complaint, supporter2)
        );

        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintSupportRepository.findByComplaintId(complaint.getId())).thenReturn(supports);

        CorroborationIntelligence result = corroborationService.getCorroboration(complaint.getId());

        assertNotNull(result);
        assertNull(result.clusterId());
        assertEquals(1, result.totalReports());
        assertEquals(1, result.uniqueCitizens());
        assertFalse(result.recurring());
        assertEquals(0, result.recurrenceCount());
        assertEquals(2L, result.supportCount());
        assertEquals(2L, result.uniqueSupporters());
        // 1 citizen = 25.0 + 2 supporters = 10.0 -> score = 35.0
        assertEquals(35.0, result.corroborationScore());
    }

    @Test
    @DisplayName("13. Expected specification example trace")
    void testExpectedSpecificationExampleTrace() {
        // Cluster X:
        // Complaint 1 -> Citizen A
        // Complaint 2 -> Citizen B
        // Complaint 3 -> Citizen A
        // Supports:
        // Citizen C -> Complaint 1
        // Citizen D -> Complaint 1
        // Citizen C -> Complaint 2
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 10, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 11, 11, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 9, 12, 12, 0);

        Complaint c1 = createComplaint(citizenA, cluster, t1);
        Complaint c2 = createComplaint(citizenB, cluster, t2);
        Complaint c3 = createComplaint(citizenA, cluster, t3);

        List<Complaint> complaints = List.of(c1, c2, c3);

        List<ComplaintSupport> supports = List.of(
                new ComplaintSupport(c1, citizenC),
                new ComplaintSupport(c1, citizenD),
                new ComplaintSupport(c2, citizenC)
        );

        when(complaintRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
        when(complaintRepository.findByClusterIdOrderByCreatedAtAsc(cluster.getId()))
                .thenReturn(complaints);
        when(complaintSupportRepository.findByComplaintIdIn(anyList()))
                .thenReturn(supports);

        CorroborationIntelligence result = corroborationService.getCorroboration(c1.getId());

        assertEquals(3, result.totalReports());
        assertEquals(2, result.uniqueCitizens());
        assertEquals(3L, result.supportCount());
        assertEquals(2L, result.uniqueSupporters());
        assertTrue(result.recurring());
        assertEquals(2, result.recurrenceCount());
        assertEquals(t1, result.firstReportedAt());
        assertEquals(t3, result.lastReportedAt());
        assertEquals(65.0, result.corroborationScore());
    }

    @Test
    @DisplayName("14. Existing M4 duplicate detection remains unchanged")
    void testExistingM4DuplicateDetectionRemainsUnchanged() {
        ComplaintDuplicateScoringService m4ScoringService = new ComplaintDuplicateScoringService();
        double m4Score = m4ScoringService.calculateCombinedScore(1.0, 1.0, 1.0, 1.0, 1.0, 0.0);
        ComplaintDuplicateScoringService.DuplicateDecision decision = m4ScoringService.decide(m4Score, true);
        assertEquals(ComplaintDuplicateScoringService.DuplicateDecision.DUPLICATE, decision);
    }
}

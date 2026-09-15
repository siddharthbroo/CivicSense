package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.ComplaintCommunityResponse;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import com.civicsense.complaint.entity.community.ComplaintCommunity;
import com.civicsense.complaint.entity.community.ComplaintSupport;
import com.civicsense.complaint.repository.ComplaintCommunityRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.repository.ComplaintSupportRepository;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintCommunityServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintSupportRepository complaintSupportRepository;

    @Mock
    private ComplaintCommunityRepository complaintCommunityRepository;

    @Mock
    private com.civicsense.complaint.repository.ComplaintImageRepository complaintImageRepository;

    @Mock
    private com.civicsense.complaint.repository.RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private CitizenCorroborationService citizenCorroborationService;

    @InjectMocks
    private ComplaintCommunityServiceImpl communityService;

    private UUID complaintId;
    private UUID userAId;
    private UUID userBId;
    private Complaint complaint;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        complaintId = UUID.randomUUID();
        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        userA = new User();
        userA.setId(userAId);
        userA.setName("Citizen A");

        userB = new User();
        userB.setId(userBId);
        userB.setName("Citizen B");

        complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setTitle("Broken streetlight");
        complaint.setDescription("Streetlight not working on 5th avenue");
        complaint.setUser(userA);
        complaint.setCreatedAt(LocalDateTime.now().minusDays(1));
        complaint.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("1. One user can support a complaint successfully")
    void testOneUserCanSupportComplaint() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(userAId)).thenReturn(Optional.of(userA));
        when(complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userAId)).thenReturn(false);
        when(complaintSupportRepository.countByComplaintId(complaintId)).thenReturn(1L);
        when(complaintCommunityRepository.findByComplaintId(complaintId)).thenReturn(Optional.empty());
        when(complaintCommunityRepository.save(any(ComplaintCommunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintCommunityResponse response = communityService.supportComplaint(complaintId, userAId);

        assertNotNull(response);
        assertEquals(complaintId, response.complaintId());
        assertEquals(1L, response.supportCount());
        assertTrue(response.supportedByCurrentUser());

        // Verify ComplaintSupport was persisted
        ArgumentCaptor<ComplaintSupport> supportCaptor = ArgumentCaptor.forClass(ComplaintSupport.class);
        verify(complaintSupportRepository, times(1)).save(supportCaptor.capture());
        assertEquals(complaint, supportCaptor.getValue().getComplaint());
        assertEquals(userA, supportCaptor.getValue().getUser());

        // Verify ComplaintCommunity was saved with count 1
        ArgumentCaptor<ComplaintCommunity> communityCaptor = ArgumentCaptor.forClass(ComplaintCommunity.class);
        verify(complaintCommunityRepository, times(1)).save(communityCaptor.capture());
        assertEquals(1L, communityCaptor.getValue().getSupportCount());
    }

    @Test
    @DisplayName("2. Same user cannot support the same complaint twice (uniqueness enforced)")
    void testSameUserCannotSupportSameComplaintTwice() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(userAId)).thenReturn(Optional.of(userA));
        when(complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userAId)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> communityService.supportComplaint(complaintId, userAId)
        );

        assertEquals("User has already supported this complaint", exception.getMessage());
        verify(complaintSupportRepository, never()).save(any());
        verify(complaintCommunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Different users can support the same complaint")
    void testDifferentUsersCanSupportSameComplaint() {
        ComplaintCommunity community = new ComplaintCommunity(complaint);
        community.setSupportCount(1L);

        // User A has already supported, now User B supports
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(userBId)).thenReturn(Optional.of(userB));
        when(complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userBId)).thenReturn(false);
        when(complaintCommunityRepository.findByComplaintId(complaintId)).thenReturn(Optional.of(community));
        when(complaintSupportRepository.countByComplaintId(complaintId)).thenReturn(2L);
        when(complaintCommunityRepository.save(any(ComplaintCommunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintCommunityResponse response = communityService.supportComplaint(complaintId, userBId);

        assertNotNull(response);
        assertEquals(complaintId, response.complaintId());
        assertEquals(2L, response.supportCount());
        assertTrue(response.supportedByCurrentUser());

        ArgumentCaptor<ComplaintSupport> supportCaptor = ArgumentCaptor.forClass(ComplaintSupport.class);
        verify(complaintSupportRepository, times(1)).save(supportCaptor.capture());
        assertEquals(userB, supportCaptor.getValue().getUser());
        assertEquals(complaint, supportCaptor.getValue().getComplaint());
    }

    @Test
    @DisplayName("4. Support count is correctly computed and exposed in community representation")
    void testSupportCountIsCorrect() {
        ComplaintCommunity community = new ComplaintCommunity(complaint);
        community.setSupportCount(5L);
        community.setCreatedAt(LocalDateTime.now().minusHours(2));
        community.setUpdatedAt(LocalDateTime.now());

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(complaintCommunityRepository.findByComplaintId(complaintId)).thenReturn(Optional.of(community));
        when(complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userAId)).thenReturn(true);

        ComplaintCommunityResponse response = communityService.getCommunityRepresentation(complaintId, userAId);

        assertNotNull(response);
        assertEquals(complaintId, response.complaintId());
        assertEquals(5L, response.supportCount());
        assertTrue(response.supportedByCurrentUser());

        // Check for an anonymous/unauthenticated user lookup
        ComplaintCommunityResponse anonResponse = communityService.getCommunityRepresentation(complaintId, null);
        assertNotNull(anonResponse);
        assertEquals(5L, anonResponse.supportCount());
        assertFalse(anonResponse.supportedByCurrentUser());
    }

    @Test
    @DisplayName("5. Support is NOT an independent complaint and does NOT mutate cluster complaintCount")
    void testSupportIsNotCountedAsIndependentComplaint() {
        ComplaintCluster cluster = new ComplaintCluster();
        cluster.setId(UUID.randomUUID());
        cluster.setComplaintCount(1);
        complaint.setCluster(cluster);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(userBId)).thenReturn(Optional.of(userB));
        when(complaintSupportRepository.existsByComplaintIdAndUserId(complaintId, userBId)).thenReturn(false);
        when(complaintSupportRepository.countByComplaintId(complaintId)).thenReturn(1L);
        when(complaintCommunityRepository.findByComplaintId(complaintId)).thenReturn(Optional.empty());
        when(complaintCommunityRepository.save(any(ComplaintCommunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        communityService.supportComplaint(complaintId, userBId);

        // Verification: Cluster complaintCount must remain 1 (untouched)
        assertEquals(1, cluster.getComplaintCount(), "Cluster complaint count must remain untouched by corroboration supports");

        // Verify Complaint entity was not saved again as a new complaint
        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Remove support decrements support count and sets supportedByCurrentUser to false")
    void testRemoveSupport() {
        ComplaintCommunity community = new ComplaintCommunity(complaint);
        community.setSupportCount(1L);

        ComplaintSupport support = new ComplaintSupport(complaint, userA);

        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(userAId)).thenReturn(Optional.of(userA));
        when(complaintSupportRepository.findByComplaintIdAndUserId(complaintId, userAId))
                .thenReturn(Optional.of(support));
        when(complaintSupportRepository.countByComplaintId(complaintId)).thenReturn(0L);
        when(complaintCommunityRepository.findByComplaintId(complaintId)).thenReturn(Optional.of(community));
        when(complaintCommunityRepository.save(any(ComplaintCommunity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintCommunityResponse response = communityService.removeSupport(complaintId, userAId);

        assertNotNull(response);
        assertEquals(complaintId, response.complaintId());
        assertEquals(0L, response.supportCount());
        assertFalse(response.supportedByCurrentUser());
        verify(complaintSupportRepository, times(1)).delete(support);
    }
}


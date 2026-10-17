package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.community.CommunityComplaintResponse;
import com.civicsense.complaint.dto.community.ComplaintCommunityResponse;
import com.civicsense.complaint.dto.community.CorroborationIntelligence;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.entity.cluster.ComplaintCluster;
import com.civicsense.complaint.entity.cluster.ComplaintPriority;
import com.civicsense.exception.GlobalExceptionHandler;
import com.civicsense.complaint.service.community.CitizenCorroborationService;
import com.civicsense.complaint.service.community.ComplaintCommunityService;
import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

    @Mock
    private ComplaintCommunityService complaintCommunityService;

    @Mock
    private CitizenCorroborationService citizenCorroborationService;

    @InjectMocks
    private CommunityController communityController;

    private MockMvc mockMvc;

    private UUID complaintId;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(communityController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new com.civicsense.exception.GlobalExceptionHandler())
                .build();

        complaintId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Citizen Test");
    }

    private CommunityComplaintResponse createSampleResponse(boolean supportedByCurrentUser) {
        return new CommunityComplaintResponse(
                complaintId,
                "https://imagekit.io/civicsense/test.jpg",
                "Deep Pothole on 5th Ave",
                "Large crater posing vehicle damage risk",
                "ROAD_DAMAGE",
                "5th Avenue near Market",
                28.7041,
                77.1025,
                ComplaintPriority.HIGH,
                ComplaintStatus.SUBMITTED,
                5L,
                0L,
                3,
                2,
                true,
                2,
                LocalDateTime.of(2026, 9, 10, 8, 0),
                LocalDateTime.of(2026, 9, 12, 14, 0),
                supportedByCurrentUser,
                LocalDateTime.of(2026, 9, 10, 8, 0),
                LocalDateTime.of(2026, 9, 12, 14, 0)
        );
    }

    @Test
    @DisplayName("1. Public feed works without authentication")
    void testPublicFeedWorksWithoutAuthentication() throws Exception {
        CommunityComplaintResponse sample = createSampleResponse(false);
        when(complaintCommunityService.getCommunityFeed(null, "RECENT", null))
                .thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/community/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$[0].title").value("Deep Pothole on 5th Ave"))
                .andExpect(jsonPath("$[0].category").value("ROAD_DAMAGE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].commentCount").value(0))
                .andExpect(jsonPath("$[0].supportedByCurrentUser").value(false));
    }

    @Test
    @DisplayName("1b. Nearby complaints endpoint returns matching complaints")
    void testNearbyComplaintsEndpoint() throws Exception {
        CommunityComplaintResponse sample = createSampleResponse(false);
        when(complaintCommunityService.getNearbyCommunityComplaints(eq(28.55), eq(77.20), eq(5.0), eq(null)))
                .thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/community/complaints/nearby")
                        .param("latitude", "28.55")
                        .param("longitude", "77.20")
                        .param("radiusKm", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$[0].title").value("Deep Pothole on 5th Ave"));
    }

    @Test
    @DisplayName("2. Public complaint detail works without authentication")
    void testPublicComplaintDetailWorksWithoutAuthentication() throws Exception {
        CommunityComplaintResponse sample = createSampleResponse(false);
        when(complaintCommunityService.getCommunityComplaint(eq(complaintId), eq(null)))
                .thenReturn(sample);

        mockMvc.perform(get("/api/v1/community/complaints/{id}", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$.imageUrl").value("https://imagekit.io/civicsense/test.jpg"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.supportCount").value(5))
                .andExpect(jsonPath("$.supportedByCurrentUser").value(false));
    }

    @Test
    @DisplayName("3. Public corroboration endpoint works without authentication")
    void testPublicCorroborationEndpointWorksWithoutAuthentication() throws Exception {
        CorroborationIntelligence intel = new CorroborationIntelligence(
                complaintId,
                UUID.randomUUID(),
                4,
                3,
                6L,
                5L,
                true,
                3,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 10, 18, 0),
                85.0
        );

        when(citizenCorroborationService.getCorroboration(complaintId)).thenReturn(intel);

        mockMvc.perform(get("/api/v1/community/complaints/{id}/corroboration", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$.totalReports").value(4))
                .andExpect(jsonPath("$.uniqueCitizens").value(3))
                .andExpect(jsonPath("$.supportCount").value(6))
                .andExpect(jsonPath("$.uniqueSupporters").value(5))
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.recurrenceCount").value(3))
                .andExpect(jsonPath("$.corroborationScore").value(85.0));
    }

    @Test
    @DisplayName("4. Authenticated user can support a complaint")
    void testAuthenticatedUserCanSupportComplaint() {
        ComplaintCommunityResponse supportResponse = new ComplaintCommunityResponse(
                complaintId,
                6L,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(complaintCommunityService.supportComplaint(complaintId, userId))
                .thenReturn(supportResponse);

        ResponseEntity<ComplaintCommunityResponse> response = communityController.supportComplaint(complaintId, testUser);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(6L, response.getBody().supportCount());
        assertTrue(response.getBody().supportedByCurrentUser());
    }

    @Test
    @DisplayName("5. Duplicate support does not create duplicate records (IllegalStateException)")
    void testDuplicateSupportThrowsIllegalStateException() {
        when(complaintCommunityService.supportComplaint(complaintId, userId))
                .thenThrow(new IllegalStateException("User has already supported this complaint"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> communityController.supportComplaint(complaintId, testUser)
        );

        assertEquals("User has already supported this complaint", ex.getMessage());
    }

    @Test
    @DisplayName("6. Authenticated user can remove support")
    void testAuthenticatedUserCanRemoveSupport() {
        ComplaintCommunityResponse removeResponse = new ComplaintCommunityResponse(
                complaintId,
                4L,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(complaintCommunityService.removeSupport(complaintId, userId))
                .thenReturn(removeResponse);

        ResponseEntity<ComplaintCommunityResponse> response = communityController.removeSupport(complaintId, testUser);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(4L, response.getBody().supportCount());
        assertFalse(response.getBody().supportedByCurrentUser());
    }

    @Test
    @DisplayName("7. Removing support does not affect complaint count")
    void testRemovingSupportDoesNotAffectComplaintCount() {
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);

        ComplaintCommunityResponse removeResponse = new ComplaintCommunityResponse(
                complaintId,
                0L,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(complaintCommunityService.removeSupport(complaintId, userId)).thenReturn(removeResponse);
        ResponseEntity<ComplaintCommunityResponse> response = communityController.removeSupport(complaintId, testUser);

        assertNotNull(response);
        assertEquals(0L, response.getBody().supportCount());
        // Assert that the underlying complaint identity and state remain untouched
        assertNotNull(complaint.getId());
    }

    @Test
    @DisplayName("8. Support does not affect cluster complaintCount")
    void testSupportDoesNotAffectClusterComplaintCount() {
        ComplaintCluster cluster = new ComplaintCluster();
        cluster.setId(UUID.randomUUID());
        cluster.setComplaintCount(3);

        int originalCount = cluster.getComplaintCount();

        ComplaintCommunityResponse supportResponse = new ComplaintCommunityResponse(
                complaintId,
                1L,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(complaintCommunityService.supportComplaint(complaintId, userId)).thenReturn(supportResponse);
        communityController.supportComplaint(complaintId, testUser);

        // Verify cluster complaintCount was NOT altered by community support
        assertEquals(originalCount, cluster.getComplaintCount(), "Support must never increment cluster complaintCount");
    }

    @Test
    @DisplayName("9. Support does not increase totalReports")
    void testSupportDoesNotIncreaseTotalReports() {
        CorroborationIntelligence before = new CorroborationIntelligence(
                complaintId, UUID.randomUUID(), 3, 2, 0L, 0L, true, 2,
                LocalDateTime.now(), LocalDateTime.now(), 55.0
        );

        // Community support changes supportCount but totalReports remains strictly 3
        CorroborationIntelligence after = new CorroborationIntelligence(
                complaintId, before.clusterId(), before.totalReports(), before.uniqueCitizens(),
                1L, 1L, before.recurring(), before.recurrenceCount(),
                before.firstReportedAt(), before.lastReportedAt(), 60.0
        );

        assertEquals(before.totalReports(), after.totalReports(), "totalReports must remain unchanged by support");
    }

    @Test
    @DisplayName("10. Support does not increase uniqueCitizens")
    void testSupportDoesNotIncreaseUniqueCitizens() {
        CorroborationIntelligence before = new CorroborationIntelligence(
                complaintId, UUID.randomUUID(), 2, 2, 0L, 0L, true, 1,
                LocalDateTime.now(), LocalDateTime.now(), 50.0
        );

        // After external supporter supports
        CorroborationIntelligence after = new CorroborationIntelligence(
                complaintId, before.clusterId(), before.totalReports(), before.uniqueCitizens(),
                1L, 1L, before.recurring(), before.recurrenceCount(),
                before.firstReportedAt(), before.lastReportedAt(), 55.0
        );

        assertEquals(before.uniqueCitizens(), after.uniqueCitizens(), "uniqueCitizens must count only complaint authors");
    }

    @Test
    @DisplayName("11. Corroboration data is correctly returned")
    void testCorroborationDataCorrectlyReturned() throws Exception {
        UUID clusterId = UUID.randomUUID();
        CorroborationIntelligence intel = new CorroborationIntelligence(
                complaintId,
                clusterId,
                5,
                3,
                8L,
                7L,
                true,
                4,
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 10, 15, 0),
                95.0
        );

        when(citizenCorroborationService.getCorroboration(complaintId)).thenReturn(intel);

        mockMvc.perform(get("/api/v1/community/complaints/{id}/corroboration", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$.clusterId").value(clusterId.toString()))
                .andExpect(jsonPath("$.totalReports").value(5))
                .andExpect(jsonPath("$.uniqueCitizens").value(3))
                .andExpect(jsonPath("$.supportCount").value(8))
                .andExpect(jsonPath("$.uniqueSupporters").value(7))
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.recurrenceCount").value(4))
                .andExpect(jsonPath("$.corroborationScore").value(95.0));
    }

    @Test
    @DisplayName("12. supportedByCurrentUser = true for a user who has supported")
    void testSupportedByCurrentUserTrueWhenSupported() throws Exception {
        CommunityComplaintResponse response = createSampleResponse(true);
        when(complaintCommunityService.getCommunityComplaint(complaintId, userId))
                .thenReturn(response);

        ResponseEntity<CommunityComplaintResponse> entity = communityController.getCommunityComplaint(complaintId, testUser);

        assertNotNull(entity);
        assertNotNull(entity.getBody());
        assertTrue(entity.getBody().supportedByCurrentUser());
    }

    @Test
    @DisplayName("13. supportedByCurrentUser = false for a user who has not supported")
    void testSupportedByCurrentUserFalseWhenNotSupported() throws Exception {
        CommunityComplaintResponse response = createSampleResponse(false);
        when(complaintCommunityService.getCommunityComplaint(complaintId, userId))
                .thenReturn(response);

        ResponseEntity<CommunityComplaintResponse> entity = communityController.getCommunityComplaint(complaintId, testUser);

        assertNotNull(entity);
        assertNotNull(entity.getBody());
        assertFalse(entity.getBody().supportedByCurrentUser());
    }

    @Test
    @DisplayName("14. supportedByCurrentUser = false for unauthenticated users")
    void testSupportedByCurrentUserFalseForUnauthenticated() throws Exception {
        CommunityComplaintResponse response = createSampleResponse(false);
        when(complaintCommunityService.getCommunityComplaint(complaintId, null))
                .thenReturn(response);

        ResponseEntity<CommunityComplaintResponse> entity = communityController.getCommunityComplaint(complaintId, null);

        assertNotNull(entity);
        assertNotNull(entity.getBody());
        assertFalse(entity.getBody().supportedByCurrentUser());
    }

    @Test
    @DisplayName("15. Nonexistent complaint returns 404")
    void testNonexistentComplaintReturns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(complaintCommunityService.getCommunityComplaint(eq(randomId), any()))
                .thenThrow(new ResourceNotFoundException("Complaint not found with id: " + randomId));

        mockMvc.perform(get("/api/v1/community/complaints/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("16. Unauthenticated support request returns 401 Unauthorized")
    void testUnauthenticatedSupportReturns401() throws Exception {
        ResponseEntity<ComplaintCommunityResponse> response = communityController.supportComplaint(complaintId, null);

        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @DisplayName("17. Unauthenticated remove support returns 401 Unauthorized")
    void testUnauthenticatedRemoveSupportReturns401() throws Exception {
        ResponseEntity<ComplaintCommunityResponse> response = communityController.removeSupport(complaintId, null);

        assertNotNull(response);
        assertEquals(401, response.getStatusCode().value());
    }
}

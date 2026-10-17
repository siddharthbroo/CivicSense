package com.civicsense.complaint.controller;

import com.civicsense.complaint.dto.community.CommunityCommentResponse;
import com.civicsense.complaint.dto.community.CreateCommentRequest;
import com.civicsense.complaint.dto.community.UpdateCommentRequest;
import com.civicsense.complaint.service.community.ComplaintCommentService;
import com.civicsense.exception.GlobalExceptionHandler;
import com.civicsense.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommunityCommentControllerTest {

    @Mock
    private ComplaintCommentService complaintCommentService;

    @InjectMocks
    private CommunityCommentController communityCommentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID complaintId;
    private UUID commentId;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(communityCommentController)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        complaintId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Priya Sharma");
        testUser.setEmail("priya@example.com");
    }

    private CommunityCommentResponse createSampleResponse(UUID id, UUID cId, String author, boolean editable) {
        return new CommunityCommentResponse(
                id,
                cId,
                "This water leak has worsened this morning.",
                author,
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 10, 0),
                editable,
                editable
        );
    }

    @Test
    @DisplayName("1. Public GET comments returns paginated comments without authentication")
    void testGetCommentsPublic() throws Exception {
        CommunityCommentResponse response = createSampleResponse(commentId, complaintId, "Citizen One", false);
        Page<CommunityCommentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(complaintCommentService.getComments(eq(complaintId), any(Pageable.class), eq(null)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/community/complaints/{complaintId}/comments", complaintId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].complaintId").value(complaintId.toString()))
                .andExpect(jsonPath("$.content[0].content").value("This water leak has worsened this morning."))
                .andExpect(jsonPath("$.content[0].authorName").value("Citizen One"))
                .andExpect(jsonPath("$.content[0].editableByCurrentUser").value(false))
                .andExpect(jsonPath("$.content[0].deletableByCurrentUser").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("2. Direct controller call to createComment returns 201 CREATED")
    void testCreateCommentDirect() {
        CreateCommentRequest request = new CreateCommentRequest("Repair crew arrived at 9 AM.");

        CommunityCommentResponse response = new CommunityCommentResponse(
                commentId,
                complaintId,
                "Repair crew arrived at 9 AM.",
                "Priya Sharma",
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 10, 0),
                true,
                true
        );
        when(complaintCommentService.createComment(eq(complaintId), eq(request), eq(testUser)))
                .thenReturn(response);

        ResponseEntity<CommunityCommentResponse> result = communityCommentController.createComment(complaintId, request, testUser);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Repair crew arrived at 9 AM.", result.getBody().content());
        assertEquals("Priya Sharma", result.getBody().authorName());
        assertTrue(result.getBody().editableByCurrentUser());
    }

    @Test
    @DisplayName("3. Direct controller call to updateComment returns 200 OK")
    void testUpdateCommentDirect() {
        UpdateCommentRequest request = new UpdateCommentRequest("Repair crew left and water is still leaking.");

        CommunityCommentResponse updatedResponse = new CommunityCommentResponse(
                commentId,
                complaintId,
                "Repair crew left and water is still leaking.",
                "Priya Sharma",
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 11, 30),
                true,
                true
        );

        when(complaintCommentService.updateComment(eq(commentId), eq(request), eq(testUser)))
                .thenReturn(updatedResponse);

        ResponseEntity<CommunityCommentResponse> result = communityCommentController.updateComment(commentId, request, testUser);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Repair crew left and water is still leaking.", result.getBody().content());
    }

    @Test
    @DisplayName("4. Direct controller call to deleteComment returns 204 NO_CONTENT")
    void testDeleteCommentDirect() {
        doNothing().when(complaintCommentService).deleteComment(eq(commentId), eq(testUser));

        ResponseEntity<Void> result = communityCommentController.deleteComment(commentId, testUser);

        assertNotNull(result);
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(complaintCommentService, times(1)).deleteComment(commentId, testUser);
    }

    @Test
    @DisplayName("5. Unauthenticated create returns 401 UNAUTHORIZED")
    void testCreateCommentUnauthenticated() {
        CreateCommentRequest request = new CreateCommentRequest("Test unauthenticated");
        ResponseEntity<CommunityCommentResponse> result = communityCommentController.createComment(complaintId, request, null);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    @DisplayName("6. Create comment with blank content fails Bean Validation")
    void testCreateCommentValidationFailsOnBlank() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("   ");

        mockMvc.perform(post("/api/v1/community/complaints/{complaintId}/comments", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Create comment with content exceeding 1000 characters fails validation")
    void testCreateCommentValidationFailsOnTooLong() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("a".repeat(1001));

        mockMvc.perform(post("/api/v1/community/complaints/{complaintId}/comments", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Update comment throwing AccessDeniedException returns 403 FORBIDDEN")
    void testUpdateCommentForbidden() {
        UpdateCommentRequest request = new UpdateCommentRequest("Trying to edit someone else's comment");

        when(complaintCommentService.updateComment(eq(commentId), eq(request), eq(testUser)))
                .thenThrow(new AccessDeniedException("You are not authorized to edit this comment"));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> communityCommentController.updateComment(commentId, request, testUser)
        );

        assertEquals("You are not authorized to edit this comment", ex.getMessage());
    }

    @Test
    @DisplayName("9. Delete comment throwing AccessDeniedException returns 403 FORBIDDEN")
    void testDeleteCommentForbidden() {
        doThrow(new AccessDeniedException("You are not authorized to delete this comment"))
                .when(complaintCommentService).deleteComment(eq(commentId), eq(testUser));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> communityCommentController.deleteComment(commentId, testUser)
        );

        assertEquals("You are not authorized to delete this comment", ex.getMessage());
    }

    @Test
    @DisplayName("10. Page size capping caps at 50 in controller")
    void testPageSizeCappedAt50() throws Exception {
        Page<CommunityCommentResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(complaintCommentService.getComments(eq(complaintId), any(Pageable.class), eq(null)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/community/complaints/{complaintId}/comments?size=100", complaintId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(complaintCommentService).getComments(eq(complaintId), argThat(p -> p.getPageSize() == 50), eq(null));
    }
}


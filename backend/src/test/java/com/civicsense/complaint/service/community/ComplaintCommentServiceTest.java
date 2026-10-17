package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CommunityCommentResponse;
import com.civicsense.complaint.dto.community.CreateCommentRequest;
import com.civicsense.complaint.dto.community.UpdateCommentRequest;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.community.ComplaintComment;
import com.civicsense.complaint.repository.ComplaintCommentRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintCommentServiceTest {

    @Mock
    private ComplaintCommentRepository complaintCommentRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintCommentServiceImpl complaintCommentService;

    private User user1;
    private User user2;
    private Complaint complaint;
    private UUID complaintId;

    @BeforeEach
    void setUp() {
        complaintId = UUID.randomUUID();

        user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setName("Ravi Rastogi");
        user1.setMobileNumber("9876543210");

        user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setName("Abhishek Sharma");
        user2.setMobileNumber("9876543211");

        complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setUser(user1);
        complaint.setTitle("Large pothole on MG Road");
        complaint.setDescription("Deep pothole causing vehicle damage.");
    }

    @Test
    @DisplayName("1. Create comment successfully with valid content")
    void createComment_Success() {
        CreateCommentRequest request = new CreateCommentRequest("This issue has been present for two weeks.");
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        UUID commentId = UUID.randomUUID();
        ComplaintComment savedComment = new ComplaintComment(complaint, user1, "This issue has been present for two weeks.");
        savedComment.setId(commentId);
        savedComment.setCreatedAt(LocalDateTime.now());
        savedComment.setUpdatedAt(LocalDateTime.now());

        when(complaintCommentRepository.save(any(ComplaintComment.class))).thenReturn(savedComment);

        CommunityCommentResponse response = complaintCommentService.createComment(complaintId, request, user1);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(commentId);
        assertThat(response.complaintId()).isEqualTo(complaintId);
        assertThat(response.content()).isEqualTo("This issue has been present for two weeks.");
        assertThat(response.authorName()).isEqualTo("Ravi Rastogi");
        assertThat(response.editableByCurrentUser()).isTrue();
        assertThat(response.deletableByCurrentUser()).isTrue();

        ArgumentCaptor<ComplaintComment> captor = ArgumentCaptor.forClass(ComplaintComment.class);
        verify(complaintCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getComplaint().getId()).isEqualTo(complaintId);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("2. Get comments returns paginated comments with newest first")
    void getComments_Success() {
        when(complaintRepository.existsById(complaintId)).thenReturn(true);

        ComplaintComment comment1 = new ComplaintComment(complaint, user1, "First comment");
        comment1.setId(UUID.randomUUID());
        comment1.setCreatedAt(LocalDateTime.now().minusHours(1));

        ComplaintComment comment2 = new ComplaintComment(complaint, user2, "Second comment");
        comment2.setId(UUID.randomUUID());
        comment2.setCreatedAt(LocalDateTime.now());

        Page<ComplaintComment> commentPage = new PageImpl<>(List.of(comment2, comment1), PageRequest.of(0, 20), 2);
        when(complaintCommentRepository.findByComplaintId(eq(complaintId), any(Pageable.class))).thenReturn(commentPage);

        Page<CommunityCommentResponse> result = complaintCommentService.getComments(complaintId, PageRequest.of(0, 20), user1);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).authorName()).isEqualTo("Abhishek Sharma");
        assertThat(result.getContent().get(0).editableByCurrentUser()).isFalse();
        assertThat(result.getContent().get(1).authorName()).isEqualTo("Ravi Rastogi");
        assertThat(result.getContent().get(1).editableByCurrentUser()).isTrue();
    }

    @Test
    @DisplayName("3. Get comments on complaint with zero comments returns empty page")
    void getComments_Empty() {
        when(complaintRepository.existsById(complaintId)).thenReturn(true);
        when(complaintCommentRepository.findByComplaintId(eq(complaintId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<CommunityCommentResponse> result = complaintCommentService.getComments(complaintId, PageRequest.of(0, 20), user1);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("4. Blank comment content is rejected with IllegalArgumentException")
    void createComment_BlankContent_Rejected() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintCommentService.createComment(complaintId, new CreateCommentRequest("   "), user1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");

        assertThatThrownBy(() -> complaintCommentService.createComment(complaintId, new CreateCommentRequest(null), user1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    @DisplayName("5. Comment content exceeding 1000 characters is rejected")
    void createComment_ExcessiveLength_Rejected() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        String longContent = "A".repeat(1001);
        assertThatThrownBy(() -> complaintCommentService.createComment(complaintId, new CreateCommentRequest(longContent), user1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");
    }

    @Test
    @DisplayName("6 & 7. Comment is linked to correct complaint and authenticated user")
    void createComment_LinkedCorrectly() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        ComplaintComment saved = new ComplaintComment(complaint, user2, "Another citizen note");
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(LocalDateTime.now());
        when(complaintCommentRepository.save(any(ComplaintComment.class))).thenReturn(saved);

        CommunityCommentResponse response = complaintCommentService.createComment(complaintId, new CreateCommentRequest("Another citizen note"), user2);

        assertThat(response.authorName()).isEqualTo("Abhishek Sharma");
        assertThat(response.complaintId()).isEqualTo(complaintId);
    }

    @Test
    @DisplayName("8. Edit own comment succeeds and updates content and timestamp")
    void updateComment_Owner_Success() {
        UUID commentId = UUID.randomUUID();
        ComplaintComment existing = new ComplaintComment(complaint, user1, "Old content");
        existing.setId(commentId);
        existing.setCreatedAt(LocalDateTime.now().minusDays(1));
        existing.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(complaintCommentRepository.findById(commentId)).thenReturn(Optional.of(existing));
        when(complaintCommentRepository.save(any(ComplaintComment.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCommentRequest request = new UpdateCommentRequest("Updated new content");
        CommunityCommentResponse response = complaintCommentService.updateComment(commentId, request, user1);

        assertThat(response.content()).isEqualTo("Updated new content");
        assertThat(existing.getContent()).isEqualTo("Updated new content");
    }

    @Test
    @DisplayName("9. Cannot edit another user's comment (throws AccessDeniedException)")
    void updateComment_NonOwner_ThrowsAccessDenied() {
        UUID commentId = UUID.randomUUID();
        ComplaintComment existing = new ComplaintComment(complaint, user1, "Ravi's content");
        existing.setId(commentId);

        when(complaintCommentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        UpdateCommentRequest request = new UpdateCommentRequest("Abhishek trying to edit");
        assertThatThrownBy(() -> complaintCommentService.updateComment(commentId, request, user2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(complaintCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Delete own comment succeeds")
    void deleteComment_Owner_Success() {
        UUID commentId = UUID.randomUUID();
        ComplaintComment existing = new ComplaintComment(complaint, user1, "To be deleted");
        existing.setId(commentId);

        when(complaintCommentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        complaintCommentService.deleteComment(commentId, user1);

        verify(complaintCommentRepository).delete(existing);
    }

    @Test
    @DisplayName("11. Cannot delete another user's comment (throws AccessDeniedException)")
    void deleteComment_NonOwner_ThrowsAccessDenied() {
        UUID commentId = UUID.randomUUID();
        ComplaintComment existing = new ComplaintComment(complaint, user1, "Ravi's comment");
        existing.setId(commentId);

        when(complaintCommentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> complaintCommentService.deleteComment(commentId, user2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(complaintCommentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("12. Nonexistent complaint returns 404 (ResourceNotFoundException)")
    void createComment_NonexistentComplaint_Throws404() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintCommentService.createComment(complaintId, new CreateCommentRequest("Test"), user1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("13. Nonexistent comment returns 404 (ResourceNotFoundException)")
    void updateComment_NonexistentComment_Throws404() {
        UUID nonExistent = UUID.randomUUID();
        when(complaintCommentRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintCommentService.updateComment(nonExistent, new UpdateCommentRequest("Update"), user1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("14. Unauthenticated create rejected (throws AccessDeniedException)")
    void createComment_Unauthenticated_ThrowsAccessDenied() {
        assertThatThrownBy(() -> complaintCommentService.createComment(complaintId, new CreateCommentRequest("Test"), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("15. Pagination bounds page size to max 50")
    void getComments_MaxPageSizeBounded() {
        when(complaintRepository.existsById(complaintId)).thenReturn(true);
        when(complaintCommentRepository.findByComplaintId(eq(complaintId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        complaintCommentService.getComments(complaintId, PageRequest.of(0, 100), user1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complaintCommentRepository).findByComplaintId(eq(complaintId), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("16. Comment count is correct")
    void getCommentCount_Success() {
        when(complaintCommentRepository.countByComplaintId(complaintId)).thenReturn(7L);

        long count = complaintCommentService.getCommentCount(complaintId);

        assertThat(count).isEqualTo(7L);
    }
}


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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintCommentServiceImpl implements ComplaintCommentService {

    private final ComplaintCommentRepository complaintCommentRepository;
    private final ComplaintRepository complaintRepository;

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 1000;

    @Override
    @Transactional
    public CommunityCommentResponse createComment(UUID complaintId, CreateCommentRequest request, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to post comments");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        String content = validateAndSanitizeContent(request != null ? request.content() : null);

        ComplaintComment comment = new ComplaintComment(complaint, currentUser, content);
        ComplaintComment saved = complaintCommentRepository.save(comment);

        return toCommunityCommentResponse(saved, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityCommentResponse> getComments(UUID complaintId, Pageable pageable, User currentUser) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint not found with id: " + complaintId);
        }

        int pageSize = Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE);
        int pageNumber = Math.max(0, pageable.getPageNumber());
        Pageable effectivePageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        return complaintCommentRepository.findByComplaintId(complaintId, effectivePageable)
                .map(comment -> toCommunityCommentResponse(comment, currentUserId));
    }

    @Override
    @Transactional
    public CommunityCommentResponse updateComment(UUID commentId, UpdateCommentRequest request, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to edit comments");
        }

        ComplaintComment comment = complaintCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this comment");
        }

        String content = validateAndSanitizeContent(request != null ? request.content() : null);
        comment.setContent(content);

        ComplaintComment updated = complaintCommentRepository.save(comment);
        return toCommunityCommentResponse(updated, currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to delete comments");
        }

        ComplaintComment comment = complaintCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this comment");
        }

        complaintCommentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(UUID complaintId) {
        return complaintCommentRepository.countByComplaintId(complaintId);
    }

    private String validateAndSanitizeContent(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be blank");
        }

        String trimmed = rawContent.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Comment content must not exceed " + MAX_CONTENT_LENGTH + " characters");
        }

        return trimmed;
    }

    private CommunityCommentResponse toCommunityCommentResponse(ComplaintComment comment, UUID currentUserId) {
        boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUser().getId());
        String authorName = (comment.getUser() != null && comment.getUser().getName() != null)
                ? comment.getUser().getName()
                : "Citizen";

        return new CommunityCommentResponse(
                comment.getId(),
                comment.getComplaint().getId(),
                comment.getContent(),
                authorName,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isOwner,
                isOwner
        );
    }
}

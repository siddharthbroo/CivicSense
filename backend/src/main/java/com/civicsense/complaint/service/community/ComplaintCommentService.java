package com.civicsense.complaint.service.community;

import com.civicsense.complaint.dto.community.CommunityCommentResponse;
import com.civicsense.complaint.dto.community.CreateCommentRequest;
import com.civicsense.complaint.dto.community.UpdateCommentRequest;
import com.civicsense.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ComplaintCommentService {

    CommunityCommentResponse createComment(UUID complaintId, CreateCommentRequest request, User currentUser);

    Page<CommunityCommentResponse> getComments(UUID complaintId, Pageable pageable, User currentUser);

    CommunityCommentResponse updateComment(UUID commentId, UpdateCommentRequest request, User currentUser);

    void deleteComment(UUID commentId, User currentUser);

    long getCommentCount(UUID complaintId);
}


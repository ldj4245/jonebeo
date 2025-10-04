package com.johnbeo.johnbeo.domain.comment.controller;

import com.johnbeo.johnbeo.domain.comment.dto.CommentResponse;
import com.johnbeo.johnbeo.domain.comment.dto.CreateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.dto.UpdateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.service.CommentService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
        @PathVariable Long postId,
        @Valid @RequestBody CreateCommentRequest request,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        if (!postId.equals(request.postId())) {
            throw new IllegalArgumentException("요청 경로와 본문 정보가 일치하지 않습니다.");
        }
        return commentService.createComment(request, principal);
    }

    @PutMapping("/comments/{id}")
    @PreAuthorize("isAuthenticated()")
    public CommentResponse updateComment(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCommentRequest request,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return commentService.updateComment(id, request, principal);
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
        @PathVariable Long id,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        commentService.deleteComment(id, principal);
    }
}

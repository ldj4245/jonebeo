package com.johnbeo.johnbeo.domain.vote.controller;

import com.johnbeo.johnbeo.domain.vote.dto.VoteRequest;
import com.johnbeo.johnbeo.domain.vote.dto.VoteSummaryResponse;
import com.johnbeo.johnbeo.domain.vote.service.VoteService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/posts/{postId}/votes")
    public VoteSummaryResponse getPostVotes(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return voteService.getPostVotes(postId, principal);
    }

    @PostMapping("/posts/{postId}/votes")
    @PreAuthorize("isAuthenticated()")
    public VoteSummaryResponse votePost(
        @PathVariable Long postId,
        @Valid @RequestBody VoteRequest request,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return voteService.votePost(postId, request.value(), principal);
    }

    @GetMapping("/comments/{commentId}/votes")
    public VoteSummaryResponse getCommentVotes(
        @PathVariable Long commentId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return voteService.getCommentVotes(commentId, principal);
    }

    @PostMapping("/comments/{commentId}/votes")
    @PreAuthorize("isAuthenticated()")
    public VoteSummaryResponse voteComment(
        @PathVariable Long commentId,
        @Valid @RequestBody VoteRequest request,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return voteService.voteComment(commentId, request.value(), principal);
    }
}

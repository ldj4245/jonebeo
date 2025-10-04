package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.vote.service.VoteService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/votes")
@RequiredArgsConstructor
public class VoteViewController {

    private final VoteService voteService;

    @PostMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public String votePost(
        @PathVariable Long postId,
        @RequestParam int value,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        handleVote(() -> voteService.votePost(postId, value, principal), redirectAttributes);
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public String voteComment(
        @PathVariable Long commentId,
        @RequestParam int value,
        @RequestParam Long postId,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        handleVote(() -> voteService.voteComment(commentId, value, principal), redirectAttributes);
        return "redirect:/posts/" + postId + "#comment-" + commentId;
    }

    private void handleVote(Runnable voteAction, RedirectAttributes redirectAttributes) {
        try {
            voteAction.run();
        } catch (IllegalArgumentException | ResourceNotFoundException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("voteError", ex.getMessage());
        }
    }
}

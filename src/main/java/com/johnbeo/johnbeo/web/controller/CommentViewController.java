package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.domain.comment.dto.CreateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.dto.UpdateCommentRequest;
import com.johnbeo.johnbeo.domain.comment.service.CommentService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentViewController {

    private final CommentService commentService;

    @PostMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    public String createComment(
        @PathVariable Long postId,
        @RequestParam(required = false) Long parentId,
        @RequestParam String content,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes attributes
    ) {
        if (!StringUtils.hasText(content)) {
            attributes.addFlashAttribute("commentError", "댓글 내용을 입력해주세요.");
            return "redirect:/posts/" + postId;
        }
        commentService.createComment(new CreateCommentRequest(postId, parentId, content.trim()), principal);
        return "redirect:/posts/" + postId + "#comments";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String updateComment(
        @PathVariable Long id,
        @RequestParam Long postId,
        @RequestParam String content,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes attributes
    ) {
        if (!StringUtils.hasText(content)) {
            attributes.addFlashAttribute("commentError", "댓글 내용을 입력해주세요.");
            return "redirect:/posts/" + postId + "#comment-" + id;
        }
        commentService.updateComment(id, new UpdateCommentRequest(content.trim()), principal);
        return "redirect:/posts/" + postId + "#comment-" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteComment(
        @PathVariable Long id,
        @RequestParam Long postId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        commentService.deleteComment(id, principal);
        return "redirect:/posts/" + postId + "#comments";
    }
}

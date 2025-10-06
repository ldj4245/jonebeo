package com.johnbeo.johnbeo.web.controller;

import com.johnbeo.johnbeo.common.exception.ResourceNotFoundException;
import com.johnbeo.johnbeo.domain.bookmark.service.BookmarkService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkViewController {

    private final BookmarkService bookmarkService;

    @PostMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public String addBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        return execute(postId, redirectAttributes, () -> bookmarkService.addBookmark(principal.getId(), postId));
    }

    @PostMapping("/posts/{postId}/remove")
    @PreAuthorize("isAuthenticated()")
    public String removeBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        return execute(postId, redirectAttributes, () -> bookmarkService.removeBookmark(principal.getId(), postId));
    }

    private String execute(Long postId, RedirectAttributes redirectAttributes, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | ResourceNotFoundException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("bookmarkError", ex.getMessage());
        }
        return "redirect:/posts/" + postId;
    }
}

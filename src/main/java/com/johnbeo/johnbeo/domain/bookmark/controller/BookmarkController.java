package com.johnbeo.johnbeo.domain.bookmark.controller;

import com.johnbeo.johnbeo.domain.bookmark.dto.BookmarkStatusResponse;
import com.johnbeo.johnbeo.domain.bookmark.service.BookmarkService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public BookmarkStatusResponse getStatus(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        boolean bookmarked = principal != null && bookmarkService.isBookmarked(principal.getId(), postId);
        long bookmarkCount = bookmarkService.countByPost(postId);
        return new BookmarkStatusResponse(bookmarked, bookmarkCount);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookmarkStatusResponse> addBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        bookmarkService.addBookmark(principal.getId(), postId);
        BookmarkStatusResponse response = new BookmarkStatusResponse(true, bookmarkService.countByPost(postId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookmarkStatusResponse> removeBookmark(
        @PathVariable Long postId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        bookmarkService.removeBookmark(principal.getId(), postId);
        BookmarkStatusResponse response = new BookmarkStatusResponse(false, bookmarkService.countByPost(postId));
        return ResponseEntity.ok(response);
    }
}

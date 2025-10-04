package com.johnbeo.johnbeo.domain.post.controller;

import com.johnbeo.johnbeo.common.response.PageResponse;
import com.johnbeo.johnbeo.domain.post.dto.CreatePostRequest;
import com.johnbeo.johnbeo.domain.post.dto.PostResponse;
import com.johnbeo.johnbeo.domain.post.dto.PostSummaryResponse;
import com.johnbeo.johnbeo.domain.post.dto.UpdatePostRequest;
import com.johnbeo.johnbeo.domain.post.service.PostService;
import com.johnbeo.johnbeo.security.model.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public PageResponse<PostSummaryResponse> getPosts(
        @RequestParam(name = "board", required = false) String boardSlug,
        @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        if (StringUtils.hasText(boardSlug)) {
            return postService.getPostsByBoardSlug(boardSlug, pageable);
        }
        return postService.getAllPosts(pageable);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.readPost(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request, @AuthenticationPrincipal MemberPrincipal principal) {
        return postService.createPost(request, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PostResponse updatePost(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePostRequest request,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return postService.updatePost(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal principal) {
        postService.deletePost(id, principal);
    }
}

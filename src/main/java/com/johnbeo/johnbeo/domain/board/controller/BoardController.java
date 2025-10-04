package com.johnbeo.johnbeo.domain.board.controller;

import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import com.johnbeo.johnbeo.domain.board.dto.CreateBoardRequest;
import com.johnbeo.johnbeo.domain.board.service.BoardService;
import com.johnbeo.johnbeo.domain.post.dto.PostSummaryResponse;
import com.johnbeo.johnbeo.domain.post.service.PostService;
import com.johnbeo.johnbeo.common.response.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final PostService postService;

    @GetMapping
    public List<BoardResponse> getBoards() {
        return boardService.getBoards();
    }

    @GetMapping("/{slug}")
    public BoardResponse getBoard(@PathVariable String slug) {
        return boardService.getBoard(slug);
    }

    @GetMapping("/{slug}/posts")
    public PageResponse<PostSummaryResponse> getBoardPosts(
        @PathVariable String slug,
        @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        return postService.getPostsByBoardSlug(slug, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse createBoard(@Valid @RequestBody CreateBoardRequest request) {
        return boardService.createBoard(request);
    }
}

package com.johnbeo.johnbeo.domain.post.dto;

import com.johnbeo.johnbeo.domain.board.dto.BoardResponse;
import java.time.Instant;

public record PostResponse(
    Long id,
    String title,
    String content,
    long viewCount,
    long bookmarkCount,
    boolean bookmarked,
    Instant createdAt,
    Instant updatedAt,
    BoardResponse board,
    PostAuthorDto author
) {
}

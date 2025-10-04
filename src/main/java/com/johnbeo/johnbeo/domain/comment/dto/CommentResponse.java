package com.johnbeo.johnbeo.domain.comment.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
    Long id,
    String content,
    Instant createdAt,
    Instant updatedAt,
    CommentAuthorDto author,
    Long parentId,
    List<CommentResponse> replies
) {
}

package com.johnbeo.johnbeo.domain.post.dto;

import java.time.Instant;

public record PostSummaryResponse(
    Long id,
    String title,
    long viewCount,
    Instant createdAt,
    PostAuthorDto author
) {
}

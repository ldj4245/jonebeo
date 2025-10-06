package com.johnbeo.johnbeo.domain.feed.dto;

import java.time.Instant;

public record HomePostCard(
    Long id,
    String title,
    String boardName,
    String boardSlug,
    String authorNickname,
    Instant createdAt,
    long viewCount,
    long commentCount,
    long voteScore
) {
}

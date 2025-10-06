package com.johnbeo.johnbeo.domain.feed.dto;

import java.time.Instant;

public record RecentCommentCard(
    Long commentId,
    String contentSnippet,
    Instant createdAt,
    Long postId,
    String postTitle,
    String boardName,
    String boardSlug,
    String authorNickname
) {
}

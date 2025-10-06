package com.johnbeo.johnbeo.domain.notice.dto;

import java.time.Instant;

public record NoticeResponse(
    Long id,
    String title,
    String content,
    int priority,
    Instant publishedAt,
    String targetUrl
) {
}

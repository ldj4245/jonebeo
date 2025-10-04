package com.johnbeo.johnbeo.domain.board.dto;

import com.johnbeo.johnbeo.domain.board.model.BoardType;
import java.time.Instant;

public record BoardResponse(
    Long id,
    String name,
    String description,
    String slug,
    BoardType type,
    Instant createdAt
) {
}

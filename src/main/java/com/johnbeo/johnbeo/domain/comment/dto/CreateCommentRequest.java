package com.johnbeo.johnbeo.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
    @NotNull Long postId,
    Long parentId,
    @NotBlank String content
) {
}

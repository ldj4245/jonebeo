package com.johnbeo.johnbeo.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
    @NotBlank String content
) {
}

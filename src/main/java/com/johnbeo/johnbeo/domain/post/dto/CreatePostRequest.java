package com.johnbeo.johnbeo.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
    @NotNull Long boardId,
    @NotBlank @Size(min = 3, max = 150) String title,
    @NotBlank String content
) {
}

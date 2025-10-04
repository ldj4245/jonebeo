package com.johnbeo.johnbeo.domain.board.dto;

import com.johnbeo.johnbeo.domain.board.model.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 200) String description,
    @NotBlank @Size(max = 100) String slug,
    @NotNull BoardType type
) {
}

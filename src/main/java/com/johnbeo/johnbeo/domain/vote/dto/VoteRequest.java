package com.johnbeo.johnbeo.domain.vote.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Integer value) {
}

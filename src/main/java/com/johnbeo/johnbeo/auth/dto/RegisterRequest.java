package com.johnbeo.johnbeo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 4, max = 50) String username,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 30) String nickname
) {
}

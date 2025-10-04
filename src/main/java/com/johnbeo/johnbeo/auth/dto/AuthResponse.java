package com.johnbeo.johnbeo.auth.dto;

public record AuthResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {
}

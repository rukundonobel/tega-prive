package com.tegaprive.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, UserDto user) {
}

package com.agripulse.backend.auth;

public record AuthResponse(String token, String tokenType, long expiresInSeconds,
                           Long userId, String fullName, String role) {}

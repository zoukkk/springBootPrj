package com.example.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}

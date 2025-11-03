package com.example.springboot4demo.security;

public record TokenResponse(String token, long expiresIn, String authority) {
}

package com.pablo.ecommerce.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}

package com.cutm.AuthApp.DTO;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDTO user
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn, UserDTO user) {
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer", // Automatically injected!
                expiresIn,
                user
        );
    }
}
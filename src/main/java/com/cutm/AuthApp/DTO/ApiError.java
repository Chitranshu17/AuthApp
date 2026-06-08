package com.cutm.AuthApp.DTO;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(
        int status, String error, String message, String path, OffsetDateTime timestamp) {
   
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(
                status.value(),               // 1. Extract the integer (e.g., 401)
                status.getReasonPhrase(),     // 2. Extract the string (e.g., "Unauthorized")
                message,
                path,
                OffsetDateTime.now(ZoneOffset.UTC) // 3. Fixed capitalization
        );
    }
}

package com.cutm.AuthApp.DTO;

import java.sql.Timestamp;

public record ErrorResponse(
        String message, int status, String error, Timestamp timestamp
) {
}

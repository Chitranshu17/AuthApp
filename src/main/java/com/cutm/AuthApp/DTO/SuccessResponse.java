package com.cutm.AuthApp.DTO;

import java.sql.Timestamp;

public record SuccessResponse(
        String message,
        int status,
        Timestamp timestamp
) {
}

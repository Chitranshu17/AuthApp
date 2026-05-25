package com.cutm.AuthApp.Helpers;

import java.util.UUID;

public class userHelper {
    public static UUID parseUUID(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid User ID format. Must be a valid UUID.");
        }
    }
}

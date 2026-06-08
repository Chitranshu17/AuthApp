package com.cutm.AuthApp.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email(message = "Please provide a valid email address")
        @NotBlank(message = "Email cannot be blank")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters")
        String password
) {
}

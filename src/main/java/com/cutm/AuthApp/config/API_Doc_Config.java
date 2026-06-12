package com.cutm.AuthApp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application built by Chitranshu Nayak",
                summary = "Authentication and Identity Management API",
                description = "A robust, stateless authentication provider featuring local login, OAuth2, and secure token rotation.",
                version = "1.0.0",
                contact = @Contact(
                        name = "Chitranshu Nayak",
                        email = "hunterboy.email@cutm.ac.in",
                        url = "https://github.com/Chitranshu17"  // Great for recruiters looking at your API!
                )
        ),
        // This line tells Swagger to require the JWT token globally for your endpoints
        security = @SecurityRequirement(name = "BearerAuth")
)
// This block actually creates the "Authorize" button and defines how the token is passed
@SecurityScheme(
        name = "BearerAuth",
        description = "Enter your JWT token here. Do NOT include the word 'Bearer'.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class API_Doc_Config {
    // The class remains completely empty! All the magic happens in the annotations above.
}
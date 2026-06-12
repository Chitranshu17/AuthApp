package com.cutm.AuthApp.Controller;

import com.cutm.AuthApp.DTO.*;
import com.cutm.AuthApp.Entity.RefreshToken;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Helpers.AuthHelper;
import com.cutm.AuthApp.Security.CookieService;
import com.cutm.AuthApp.Security.JwtService;
import com.cutm.AuthApp.Services.AuthService;
import com.cutm.AuthApp.Services.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthHelper authHelper;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final PasswordRecoveryService passwordRecoveryService;
    private final CookieService cookieService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        // 1. Authenticate the user (This instantly throws an exception if the password is wrong)
        Authentication authenticated = authHelper.authenticate(loginRequest);

        // 2. Extract the fully loaded User entity from the Authentication Principal
        User user = (User) authenticated.getPrincipal();

        if (!user.isEnabled()) {
            throw new DisabledException("User is Disabled");
        }

        String jti = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .createdDate(now)
                .expiryDate(now.plusSeconds(jwtService.getRefreshTtlSeconds()))
                .user(user)
                .revoked(false)
                .build();

        authService.saveRefreshToken(refreshTokenEntity);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenEntity.getJti());

        cookieService.attachRefreshTokenCookie(response, refreshToken);
        cookieService.addNoCacheHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDTO.class));

        // 4. Return the 200 OK with your clean, formatted JSON
        return ResponseEntity.ok(tokenResponse);
    }

    // Access and Refresh Token renew Api
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        // 1. The Helper checks the Cookie Vault, Header, and JSON body
        String oldRefreshTokenString = authHelper.readRefreshTokenFromRequest(body, request);

        // 2. The Service Layer does ALL the heavy lifting, database checks, and token generation
        TokenResponse tokenResponse = authService.rotateRefreshToken(oldRefreshTokenString);

        // 3. We take the newly generated token from the DTO and lock it in the browser's cookie vault
        cookieService.attachRefreshTokenCookie(response, tokenResponse.refreshToken());
        cookieService.addNoCacheHeaders(response);

        // 4. Return the 200 OK
        return ResponseEntity.ok(tokenResponse);
    }

//    @PostMapping("/logout")
//    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
//
//        // 1. Extract the token from the request
//        String refreshTokenString = authHelper.readRefreshTokenFromRequest(null, request);
//
//        // 2. Kill the token in the database
//        try {
//            if (refreshTokenString != null && !refreshTokenString.isBlank()) {
//                authService.revokeRefreshToken(refreshTokenString);
//            }
//        } catch (Exception ignored) {
//            // If the token is already dead or malformed, we just ignore the error.
//            // The goal is to log them out, and a dead token means they already are!
//        }
//
//        // 3. CRITICAL: Command the browser's Vault to instantly delete the cookie
//        cookieService.clearRefreshTokenCookie(response);
//
//        // 4. CRITICAL: Return a 204 No Content (Standard HTTP status for successful logout)
//        return ResponseEntity.noContent().build();
//    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenString = authHelper.readRefreshTokenFromRequest(null, request);

        // ADD THIS PRINT LINE
        System.out.println("LOGOUT ATTEMPT - Token found: " + refreshTokenString);

        try {
            if (refreshTokenString != null && !refreshTokenString.isBlank()) {
                authService.revokeRefreshToken(refreshTokenString);
                System.out.println("LOGOUT SUCCESS - Database updated!");
            } else {
                System.out.println("LOGOUT WARNING - No token found to revoke.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // THIS WILL PRINT THE REAL ERROR
        }

        cookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = authService.registerUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordRecoveryService.processForgotPassword(request.email());
        return ResponseEntity.ok("If an account with that email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.processResetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok("Password successfully reset. You can now log in with your new password.");
    }
}

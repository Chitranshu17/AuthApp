package com.cutm.AuthApp.Helpers;

import com.cutm.AuthApp.DTO.LoginRequest;
import com.cutm.AuthApp.DTO.RefreshTokenRequest;
import com.cutm.AuthApp.Entity.Provider;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    // Notice the updated path below!
    @Value("${spring.security.jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    public Authentication authenticate(LoginRequest loginRequest) {
        // 1. Fetch the user from the database by email BEFORE trying to authenticate
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // 2. THE VALIDATION CHECK
        // If the user's provider is NOT "LOCAL" (meaning it's Google or GitHub)
        if (user.getProvider() != Provider.LOCAL) {
            // Format a nice message for the frontend to display
            String providerName = user.getProvider().name().toLowerCase(); // e.g., "google"
            providerName = providerName.substring(0, 1).toUpperCase() + providerName.substring(1); // "Google"

            throw new RuntimeException("It looks like you signed up with " + providerName + ". Please use the '" + providerName + "' login button.");
        }

        // 3. If they are a LOCAL user, proceed with normal password authentication
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public String readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {

        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)    //This is the unboxer, rips open the Array and places every single Cookie object onto the conveyor belt one by one
                .filter(cookie -> refreshTokenCookieName.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElseGet(() -> Optional.ofNullable(body)
                        .map(RefreshTokenRequest::refreshToken)
                        .orElse(null)
                );
    }
}
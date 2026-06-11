package com.cutm.AuthApp.Helpers;

import com.cutm.AuthApp.DTO.LoginRequest;
import com.cutm.AuthApp.DTO.RefreshTokenRequest;
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

    // Notice the updated path below!
    @Value("${spring.security.jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    public Authentication authenticate(LoginRequest loginRequest) {
        try {
            // Added 'return' to pass the successful authentication object back to the controller
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
package com.cutm.AuthApp.Helpers;

import com.cutm.AuthApp.DTO.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    private final AuthenticationManager authenticationManager;

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
}
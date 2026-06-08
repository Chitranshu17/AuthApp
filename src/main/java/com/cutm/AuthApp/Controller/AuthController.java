package com.cutm.AuthApp.Controller;

import com.cutm.AuthApp.DTO.LoginRequest;
import com.cutm.AuthApp.DTO.TokenResponse;
import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Entity.RefreshToken;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Helpers.AuthHelper;
import com.cutm.AuthApp.Repository.RefreshTokenRepository;
import com.cutm.AuthApp.Security.JwtService;
import com.cutm.AuthApp.Services.AuthService;
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
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

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

        refreshTokenRepository.save(refreshTokenEntity);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenEntity.getJti());
        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDTO.class));

        // 4. Return the 200 OK with your clean, formatted JSON
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = authService.registerUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }
}

package com.cutm.AuthApp.ServiceImpl;

import com.cutm.AuthApp.DTO.TokenResponse;
import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Entity.RefreshToken;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Repository.RefreshTokenRepository;
import com.cutm.AuthApp.Repository.UserRepository;
import com.cutm.AuthApp.Security.JwtService;
import com.cutm.AuthApp.Services.AuthService;
import com.cutm.AuthApp.Services.EmailService;
import com.cutm.AuthApp.Services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        UserDTO userDTO1 = userService.createUser(userDTO);
        return userDTO1;
    }

    @Override
    public void saveRefreshToken(RefreshToken token) {
        refreshTokenRepository.save(token);
    }

    // Inside AuthServiceImpl
    @Override
    public RefreshToken findByJti(String jti) { // <-- Renamed!
        return refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found in database"));
    }

    @Override
    @Transactional
    public TokenResponse rotateRefreshToken(String oldRefreshTokenString) {

        // 1. Null and Type Checks
        if (oldRefreshTokenString == null || oldRefreshTokenString.isBlank()) {
            throw new IllegalArgumentException("Refresh Token is missing or invalid");
        }
        if (!jwtService.isRefreshToken(oldRefreshTokenString)) {
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        // 2. Signature and DB Fetch
        String jti = jwtService.getJti(oldRefreshTokenString);
        RefreshToken oldTokenEntity = findByJti(jti);

        // 3. The Trap & Expiration Check
        if (oldTokenEntity.isRevoked()) {
            throw new IllegalArgumentException("Token reuse detected. Please log in again.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (oldTokenEntity.getExpiryDate().isBefore(now)) {
            throw new IllegalArgumentException("Refresh token expired in database. Please log in again.");
        }

        // --- THE ROTATION ---
        User user = oldTokenEntity.getUser();
        String newJti = UUID.randomUUID().toString();

        // Generate keys
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenString = jwtService.generateRefreshToken(user, newJti);

        // Revoke old and save
        oldTokenEntity.setRevoked(true);
        oldTokenEntity.setReplacedByToken(newJti);
        saveRefreshToken(oldTokenEntity);

        // Create new and save
        RefreshToken newTokenEntity = RefreshToken.builder()
                .jti(newJti)
                .createdDate(now)
                .expiryDate(now.plusSeconds(jwtService.getRefreshTtlSeconds()))
                .user(user)
                .revoked(false)
                .build();
        saveRefreshToken(newTokenEntity);

        // Build and return the final DTO
        return TokenResponse.of(
                newAccessToken,
                newRefreshTokenString,
                jwtService.getAccessTtlSeconds(),
                modelMapper.map(user, UserDTO.class)
        );
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        try {
            // 1. Ask the JwtService to extract the unique ID (JTI) from the string
            String jti = jwtService.getJti(token);

            // 2. Find that exact token in the database
            RefreshToken tokenEntity = findByJti(jti);

            // 3. Flip the kill switch to true and save it
            tokenEntity.setRevoked(true);
            saveRefreshToken(tokenEntity);

        } catch (Exception e) {
            // Why an empty catch block?
            // If the token is already expired, malformed, or missing from the DB,
            // the JJWT parser or database will throw an error.
            // But since the user is trying to log out anyway, a dead/missing token
            // means our goal is already achieved! We just ignore the error and let them log out.
        }
    }

}

/* The "Phone Call" Rule (Fixing the Crash)
As you saw earlier, without this annotation, your app threw a LazyInitializationException.

Think of a database connection like a phone call.

Without @Transactional: When you called findByJti(jti), Spring made a quick phone call to the database,
 grabbed the token, and immediately hung up the phone. A few lines later, when jwtService tried
 to read the User roles attached to that token, it realized it didn't have them yet.
 It tried to ask the database, but the phone was already hung up! That caused the crash.

With @Transactional: You are telling Spring, "Pick up the phone at the start of this method,
and DO NOT hang up until the method is completely finished." Because the line stays open,
Hibernate can safely reach back into the database to grab the User's roles at the
exact moment the JwtService asks for them. */

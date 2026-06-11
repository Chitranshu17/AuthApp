package com.cutm.AuthApp.Security;

import com.cutm.AuthApp.Entity.Provider;
import com.cutm.AuthApp.Entity.RefreshToken;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Repository.RefreshTokenRepository;
import com.cutm.AuthApp.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository; // Injected to save the user directly
    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Override
    // Fixed the duplicate 'throws IOException' from your snippet
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;

        // Extract the fully populated OAuth2User object
        OAuth2User oAuth2User = authToken.getPrincipal();

        String registrationId = authToken.getAuthorizedClientRegistrationId();
        logger.info("OAuth2 Authentication Success! Identity Provider: {}", registrationId);

        // Variables to hold the extracted data
        String email = null;
        String name = null;
        String image = null;

        switch (registrationId.toLowerCase()) {
            case "google":
                // Google's specific attribute names
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
                image = oAuth2User.getAttribute("picture");
                break;

            case "github":
                email = oAuth2User.getAttribute("email");
                if (email == null) {
                    String username = oAuth2User.getAttribute("login");
                    email = username + "@github.user";
                    logger.warn("GitHub email was private. Generated fallback email: {}", email);
                }

                name = oAuth2User.getAttribute("name");
                // GitHub users might not have a display name set, fallback to username
                if (name == null) name = oAuth2User.getAttribute("login");

                // GitHub uses 'avatar_url' instead of 'picture'
                image = oAuth2User.getAttribute("avatar_url");
                break;

            default:
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
        }

        if (email == null || email.isBlank()) {
            logger.error("OAuth2 provider {} did not supply an email address.", registrationId);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by identity provider");
            return;
        }

        // 2. Find or Register the user using your Builder logic
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
        } else {
            Provider authProvider = registrationId.equalsIgnoreCase("github")
                    ? Provider.GITHUB
                    : Provider.GOOGLE;
            // Building the user exactly as you mapped it out in your TODO!
            user = User.builder()
                    .email(email)
                    .name(name)
                    .isEnabled(true)
                    .provider(authProvider)
                    .image(image)
                    .build();

            // Save the user using the repository
            user = userRepository.save(user);
            logger.info("New user registered via {}: {}", registrationId, email);
        }

        // 3. Create a Refresh Token entity
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .createdDate(OffsetDateTime.now(ZoneOffset.UTC))
                .expiryDate(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(jwtService.getRefreshTtlSeconds()))
                .user(user)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 4. Generate your JWTs
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        // 5. Attach the cookie
        cookieService.attachRefreshTokenCookie(response, refreshToken);

        logger.info("Successful authentication for {}", email);

        // Redirect to your frontend application
        // We use sendRedirect here so the browser actually navigates to your React/Frontend app
        response.getWriter().write("Login successful");
    }
}
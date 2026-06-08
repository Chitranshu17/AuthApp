package com.cutm.AuthApp.Security;

import com.cutm.AuthApp.Helpers.userHelper;
import com.cutm.AuthApp.Repository.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // 1. Open the token just ONCE
                Jws<Claims> parse = jwtService.parse(token);
                Claims payload = parse.getPayload();

                // 2. Performance Check: Make sure it's an access token using the payload we just pulled
                if ("access".equals(payload.get("typ"))) {

                    String userId = payload.getSubject();
                    UUID userUUID = userHelper.parseUUID(userId);

                    // 3. Ensure the security context isn't already set by a previous filter
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {

                        userRepository.findById(userUUID).ifPresent(user -> {

                            // 4. Only authenticate if the user IS enabled
                            if (user.isEnabled()) {

                                List<GrantedAuthority> authorities = user.getRoles() == null
                                        ? List.of()
                                        : user.getRoles().stream()
                                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                                        .collect(Collectors.toList());

                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        authorities
                                );

                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        });
                    }
                }
            } catch (ExpiredJwtException e) {
                // 5a. Set the sticky note for the frontend and log it for yourself
                request.setAttribute("error", "Token Expired");
                logger.error("JWT Expired: " + e.getMessage());

            } catch (MalformedJwtException | SignatureException e) {
                // 5b. Catch forged or corrupted tokens separately
                request.setAttribute("error", "Invalid Token Signature");
                logger.error("JWT Invalid: " + e.getMessage());

            } catch (Exception e) {
                // 5c. Catch-all for any other token processing failures
                request.setAttribute("error", "Authentication failed");
                logger.error("Authentication error: " + e.getMessage());
            }
        }

        // 5. CRITICAL FIX: The one and only filterChain call must go at the absolute bottom.
        // This ensures every request (valid token, invalid token, or no token at all) safely moves to the next step.
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/error");
    }
}

// JWS (JWS (JSON Web Signature)) is the cryptographic mathematical process used to secure the token,

/* JWT (JSON Web Token)
What is it?
The JWT is the final, finished product.
 It is an open, industry-standard format (RFC 7519) for transmitting information securely between two parties as a JSON object. When you see that long,
 three-part string separated by dots (header.payload.signature) in a "Bearer" header, you are looking at a JWT.*/
package com.cutm.AuthApp.Security;

import com.cutm.AuthApp.Entity.Role;
import com.cutm.AuthApp.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Getter
@Setter
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secretKeyString,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        // Convert the raw string from YAML into a cryptographic key immediately
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }


    /// If you are passing only User then generate Access Token else Refresh Token
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())   //jti

                .subject(user.getId().toString())   // Database User Id

                // Add our custom roles list into the token payload
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access"
                ))

                // Set the creation time
                .issuedAt(Date.from(now))

                .issuer(issuer)

                // Set expiration
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))

                // Sign it with your heavily encrypted secret key
                .signWith(secretKey)

                // Compress it into the final Base64 string
                .compact();
    }

    public String generateRefreshToken(User user, String jti) {
        Instant now = Instant.now();

        return Jwts.builder()
                // Inject the unique JWT ID to prevent replay attacks
                .id(jti)

                // Set the primary identifier
                .subject(user.getId().toString())

                // Mark this specifically as a refresh token
                .claims(Map.of(
                        "typ", "refresh"
                ))

                // Set the creation time
                .issuedAt(Date.from(now))

                .issuer(issuer)

                // Set expiration (using the longer refresh TTL from your YAML)
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))

                // Sign it with your heavily encrypted secret key
                .signWith(secretKey)

                // Compress it into the final Base64 string
                .compact();
    }

    public Jws<Claims> parse(String token) {
        // Let the native exceptions (like ExpiredJwtException) bubble up!
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public boolean isAccessToken(String token) {
        Claims c = parse(token).getPayload();

        // Retrieves the "typ" claim and checks if it exactly matches "access"
        return "access".equals(c.get("typ"));
    }

    public boolean isRefreshToken(String token) {
        Claims c = parse(token).getPayload();

        // Retrieves the "typ" claim and checks if it exactly matches "refresh"
        return "refresh".equals(c.get("typ"));
    }

    public UUID getUserId(String token) {
        Claims c = parse(token).getPayload();
        return UUID.fromString(c.getSubject());
    }

    public String getJti(String token) {
        return parse(token).getPayload().getId();
    }

    public List<String> getRoles(String token) {
        Claims c = parse(token).getPayload();
        return (List<String>) c.get("roles");
    }


}


/* A parser is basically a combination of a Translator and a Security Guard (Bouncer).

The Translator's Job: It takes that giant string, chops it into pieces,
 decodes the Base64 gibberish, and turns it back into a readable Java Object (called Claims)
  so your application can actually read the user's email and roles.

The Bouncer's Job: Before it lets your application read anything,
it checks the cryptographic signature to make sure a hacker didn't tamper with the letter while it was in transit.

 */

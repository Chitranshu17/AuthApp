package com.cutm.AuthApp.Security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Getter
public class CookieService {

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final int cookieMaxAge;
    private final String cookieDomain;
    private final String cookieSameSite;

    // The constructor automatically pulls values from your application.yml on startup
    public CookieService(
            @Value("${security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${security.jwt.cookie-http-only}") boolean cookieHttpOnly,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            // We use your refresh token's TTL as the cookie's Max-Age so they die at the exact same time
            @Value("${security.jwt.refresh-ttl-seconds}") int cookieMaxAge,
            // The colon ":" at the end means "if this is missing in the YAML, default to an empty string"
            @Value("${security.jwt.cookie-domain:}") String cookieDomain,
            @Value("${security.jwt.cookie-same-site}") String cookieSameSite
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieMaxAge = cookieMaxAge;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }

    /**
     * Generates the secure HttpOnly cookie containing the refresh token.
     */
    public void attachRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .maxAge(cookieMaxAge)
                .sameSite(cookieSameSite)
                .path("/api/auth");

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie responseCookie = builder.build();

        // Attaches it directly to the raw HTTP response!
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    /**
     * Instantly destroys the refresh token cookie in the user's browser.
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0); // 0 forces the browser to instantly delete it

        // To delete a cookie, the domain and path MUST match exactly how it was created
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }

          /* The builder.build() packages up all the rules you just wrote.
         Then, response.addHeader(...) attaches this package to the HTTP response going back to the frontend. */
        ResponseCookie responseCookie = builder.build();

        // Attach the "kill command" to the response header
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    /**
     * Prevents the browser from caching sensitive JSON responses (like Access Tokens).
     */
    public void addNoCacheHeaders(HttpServletResponse response) {
        // "no-store" is the absolute strictest rule. It tells the browser: "Do not save this anywhere."
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");

        // Added for backward compatibility with older browsers (HTTP/1.0)
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
    }
}
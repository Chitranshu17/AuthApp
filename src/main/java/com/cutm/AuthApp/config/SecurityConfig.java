package com.cutm.AuthApp.config;

import com.cutm.AuthApp.Security.JwtAuthenticationEntryPoint;
import com.cutm.AuthApp.Security.JwtAuthenticationFilter;
import com.cutm.AuthApp.Security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Inject your newly created entry point
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )

                // Add your custom exception handler here, this is for Unauthorized Protected apis
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        ///  TODO setup
        // ✅ Use environment variable in production — never hardcode
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",           // dev
                "https://yourdomain.com"           // prod
        ));

        // ✅ Explicit methods only — never add "TRACE" or "CONNECT"
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // ✅ Explicitly list only what your app actually needs
        configuration.setAllowedHeaders(List.of(
                "Authorization",        // JWT Bearer token
                "Content-Type",         // application/json
                "X-Requested-With",     // AJAX requests
                "Accept",               // Response format
                "Origin",               // CORS origin header
                "Access-Control-Request-Method",   // Preflight
                "Access-Control-Request-Headers"   // Preflight
        ));

        // ✅ Expose headers the frontend JS needs to READ
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // ✅ Required for HTTP-Only cookies (refresh tokens)
        configuration.setAllowCredentials(true);

        // ✅ Cache preflight response for 1 hour — reduces OPTIONS requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


//    public UserDetailsService users(){
//      org.springframework.security.core.userdetails.User.builder() =  User.withDefaultPasswordEncoder();
//        UserDetails user1 = use
//
//    }
}


/// HttpSecurity http This is the builder object provided by Spring that allows you to configure your web-based security at a highly granular level.
// Cross-Origin Resource Sharing (CORS) using default settings.
//
/// Why for APIs: Browsers have a strict security rule: a frontend running
// on localhost:3000 is not allowed to talk to a backend on localhost:8080 unless the backend explicitly says it is okay.
// This line tells Spring to look for a @CorsConfiguration (or allow defaults) so your frontend can actually reach your API.

// By forcing statelessness,
// you guarantee that every single request must carry its own proof of identity (like an HTTP Basic header or a JWT Bearer token)

/* "Take my custom JWT Bouncer and put him in the hallway directly in front of the default Old-School Bouncer."

Now, the flow works perfectly:

The request enters the hallway carrying a Bearer token.

It hits your JWT Bouncer first.

Your bouncer sees the token, validates it, and tells the Spring Security Context, "This guy is good. He is authenticated."

The request moves to the next checkpoint (the Old-School Bouncer).

The Old-School Bouncer sees that the user is already authenticated by the previous guy, so he just steps aside and lets the request pass straight through to your API. */


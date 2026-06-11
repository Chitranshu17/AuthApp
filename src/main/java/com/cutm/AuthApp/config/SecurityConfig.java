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
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(null)
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


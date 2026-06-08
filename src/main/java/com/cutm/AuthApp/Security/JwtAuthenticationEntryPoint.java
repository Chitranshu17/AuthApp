package com.cutm.AuthApp.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // 1. Set the standard REST API response headers
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Status

        // 2. Build a clean, structured JSON error body
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "You must provide a valid JWT to access this endpoint. " + authException.getMessage());
        body.put("path", request.getServletPath());

        // 3. Convert the Map into actual JSON and write it directly to the response output stream
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}

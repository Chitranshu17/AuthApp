package com.cutm.AuthApp.Exception;

import com.cutm.AuthApp.DTO.ApiError;
import com.cutm.AuthApp.DTO.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.Timestamp;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            AuthenticationException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiError> handleAuthException(Exception e, HttpServletRequest request) {
        // Utilizing new static factory method!
        ApiError apiError = ApiError.of(
                HttpStatus.UNAUTHORIZED,
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    // 1. Handles missing users (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                new Timestamp(System.currentTimeMillis())
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 2. Handles duplicate emails (409)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                new Timestamp(System.currentTimeMillis())
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // 3. Handles bad input like missing emails or invalid UUIDs (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                new Timestamp(System.currentTimeMillis())
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
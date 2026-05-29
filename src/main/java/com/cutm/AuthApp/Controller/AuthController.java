package com.cutm.AuthApp.Controller;

import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = authService.registerUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }
}

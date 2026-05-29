package com.cutm.AuthApp.ServiceImpl;

import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Services.AuthService;
import com.cutm.AuthApp.Services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        UserDTO userDTO1 = userService.createUser(userDTO);
        return userDTO1;
    }
}

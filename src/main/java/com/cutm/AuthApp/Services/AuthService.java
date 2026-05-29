package com.cutm.AuthApp.Services;

import com.cutm.AuthApp.DTO.UserDTO;

public interface AuthService {
    UserDTO registerUser(UserDTO userDTO);
}

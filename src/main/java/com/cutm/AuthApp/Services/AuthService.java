package com.cutm.AuthApp.Services;

import com.cutm.AuthApp.DTO.TokenResponse;
import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Entity.RefreshToken;

public interface AuthService {
    UserDTO registerUser(UserDTO userDTO);

    void saveRefreshToken(RefreshToken token);

    RefreshToken findByJti(String jti);

    TokenResponse rotateRefreshToken(String oldRefreshTokenString);

    void revokeRefreshToken(String token);
    
}

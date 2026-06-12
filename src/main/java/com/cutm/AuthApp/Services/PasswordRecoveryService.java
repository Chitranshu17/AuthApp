package com.cutm.AuthApp.Services;

public interface PasswordRecoveryService {
    void processForgotPassword(String email);

    void processResetPassword(String token, String newPassword);
}
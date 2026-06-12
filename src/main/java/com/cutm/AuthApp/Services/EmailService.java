package com.cutm.AuthApp.Services;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);

}

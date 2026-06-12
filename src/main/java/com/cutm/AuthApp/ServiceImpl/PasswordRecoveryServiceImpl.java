package com.cutm.AuthApp.ServiceImpl;

import com.cutm.AuthApp.Entity.Provider;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Exception.ResourceNotFoundException;
import com.cutm.AuthApp.Repository.UserRepository;
import com.cutm.AuthApp.Security.JwtService;
import com.cutm.AuthApp.Services.EmailService;
import com.cutm.AuthApp.Services.PasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this email"));

        if (user.getProvider() != Provider.LOCAL) {
            String providerName = user.getProvider().name().toLowerCase();
            throw new IllegalArgumentException("You signed up with " + providerName + ". Please use social login.");
        }

        String resetToken = jwtService.generatePasswordResetToken(user);
        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public void processResetPassword(String token, String newPassword) {
        String email = jwtService.parse(token).getPayload().getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!jwtService.validatePasswordResetToken(token, user)) {
            throw new IllegalArgumentException("Invalid or expired reset token. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
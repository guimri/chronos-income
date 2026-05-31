package com.chronosincome.service;

import com.chronosincome.config.JwtService;
import com.chronosincome.dto.request.*;
import com.chronosincome.dto.response.AuthResponse;
import com.chronosincome.entity.User;
import com.chronosincome.exception.BusinessException;
import com.chronosincome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /** Duração do token de reset: 1 hora */
    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Este e-mail já está cadastrado");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .build();

        userRepository.save(user);

        UserDetails userDetails = buildUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        UserDetails userDetails = buildUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Logout stateless: o token JWT continua válido até expirar no cliente.
     * O front-end é responsável por descartar o token do armazenamento local.
     */
    public void logout() {
        // Nenhuma ação server-side necessária em arquitetura stateless JWT.
        // Para invalidação antes do vencimento, implemente blocklist (Redis, etc.).
    }

    /**
     * Gera token de reset e "envia" por e-mail.
     * Em dev, o token é logado no console. Integre JavaMailSender em produção.
     * Sempre retorna sem erro mesmo se o e-mail não existir (anti-enumeração).
     */
    public void forgotPassword(ForgotPasswordRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
            userRepository.save(user);

            // TODO: substituir pelo envio real via JavaMailSender
            log.info("=== [DEV] Link de recuperação de senha ===");
            log.info("E-mail: {}", user.getEmail());
            log.info("Token : {}", token);
            log.info("Link  : http://localhost:5173/reset-password?token={}", token);
            log.info("==========================================");
        });
    }

    /**
     * Valida o token de reset e redefine a senha do usuário.
     */
    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Token inválido ou já utilizado"));

        if (user.getResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new BusinessException("Token expirado. Solicite um novo link de recuperação");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}

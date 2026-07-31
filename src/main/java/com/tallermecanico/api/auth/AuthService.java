package com.tallermecanico.api.auth;

import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {
    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SystemUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SystemUser user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> invalidCredentials());
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        user.setLastLoginAt(Instant.now());
        String token = jwtService.createToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationInstant(), toAuthenticatedUser(user));
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser(String username) {
        SystemUser user = userRepository.findByUsernameIgnoreCase(username)
                .filter(SystemUser::isActive)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));
        return toAuthenticatedUser(user);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
    }

    private AuthenticatedUserResponse toAuthenticatedUser(SystemUser user) {
        return new AuthenticatedUserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole().getName());
    }
}

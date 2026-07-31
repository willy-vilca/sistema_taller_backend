package com.tallermecanico.api.user;

import com.tallermecanico.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final SystemUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SystemUserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .sorted((first, second) -> first.getFullName().compareToIgnoreCase(second.getFullName()))
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findActiveEmployees() {
        return userRepository.findAllByActiveTrueOrderByFullNameAsc().stream()
                .filter(user -> user.getRole().getName() == RoleName.EMPLEADO)
                .map(UserMapper::toSummary)
                .toList();
    }

    public UserResponse create(UserCreateRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }

        SystemUser user = new SystemUser(
                username,
                normalizeName(request.fullName()),
                passwordEncoder.encode(request.password()),
                getRole(request.role())
        );
        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserResponse update(UUID id, UserUpdateRequest request, String currentUsername) {
        SystemUser user = getEntity(id);
        String username = normalizeUsername(request.username());
        if (!user.getUsername().equalsIgnoreCase(username) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }
        if (user.getUsername().equalsIgnoreCase(currentUsername) && request.role() != RoleName.ADMIN) {
            throw new BusinessException(HttpStatus.CONFLICT, "No puedes quitarte tu propio rol de administrador.");
        }

        boolean usernameChanged = !user.getUsername().equalsIgnoreCase(username);
        user.setUsername(username);
        user.setFullName(normalizeName(request.fullName()));
        user.setRole(getRole(request.role()));
        if (usernameChanged) {
            user.invalidateSessions();
        }
        return UserMapper.toResponse(user);
    }

    public UserResponse updateStatus(UUID id, UserStatusUpdateRequest request, String currentUsername) {
        SystemUser user = getEntity(id);
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !request.active()) {
            throw new BusinessException(HttpStatus.CONFLICT, "No puedes desactivar tu propio acceso.");
        }
        if (user.isActive() != request.active()) {
            user.setActive(request.active());
            user.invalidateSessions();
        }
        return UserMapper.toResponse(user);
    }

    public void deactivate(UUID id, String currentUsername) {
        updateStatus(id, new UserStatusUpdateRequest(false), currentUsername);
    }

    public void updatePassword(UUID id, PasswordUpdateRequest request) {
        SystemUser user = getEntity(id);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.invalidateSessions();
    }

    @Transactional(readOnly = true)
    public SystemUser getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));
    }

    @Transactional(readOnly = true)
    public SystemUser getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el usuario solicitado."));
    }

    private Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Los roles del sistema no están configurados."));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}

package com.tallermecanico.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El usuario es obligatorio.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "El usuario debe tener entre 3 y 50 caracteres válidos.")
        String username,
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String fullName,
        @Email(message = "Ingresa un correo electrónico válido.")
        @Size(max = 254, message = "El correo electrónico no puede superar 254 caracteres.")
        String email,
        @NotNull(message = "El rol es obligatorio.")
        RoleName role,
        boolean scheduleNotificationsEnabled
) {
}

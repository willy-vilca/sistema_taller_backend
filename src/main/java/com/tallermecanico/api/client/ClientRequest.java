package com.tallermecanico.api.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 120, message = "El nombre completo no puede superar 120 caracteres.")
        String fullName,
        @NotBlank(message = "El DNI es obligatorio.")
        @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener 8 dígitos.")
        String dni,
        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres.")
        String phone,
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
        String email
) {
}

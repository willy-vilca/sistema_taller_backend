package com.tallermecanico.api.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank(message = "La placa es obligatoria.")
        @Pattern(regexp = "^[a-zA-Z0-9-]{5,10}$", message = "Ingresa una placa válida.")
        String licensePlate,
        @NotBlank(message = "El modelo es obligatorio.")
        @Size(max = 120, message = "El modelo no puede superar 120 caracteres.")
        String model
) {
}

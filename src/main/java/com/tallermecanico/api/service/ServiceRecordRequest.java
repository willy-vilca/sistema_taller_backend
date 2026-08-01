package com.tallermecanico.api.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceRecordRequest(
        @NotNull(message = "Debes seleccionar un vehículo.") UUID vehicleId,
        @NotNull(message = "Debes seleccionar al mecánico responsable.") UUID responsibleUserId,
        @NotBlank(message = "La descripción del trabajo es obligatoria.")
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres.")
        String description,
        @NotNull(message = "La fecha del servicio es obligatoria.") LocalDate serviceDate,
        LocalDate nextServiceDate,
        @Size(max = 2000, message = "La descripción del próximo servicio no puede superar 2000 caracteres.")
        String nextServiceDescription,
        UUID scheduledServiceId,
        @DecimalMin(value = "0.00", message = "El costo no puede ser negativo.") BigDecimal totalCost,
        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres.") String notes
) {
}

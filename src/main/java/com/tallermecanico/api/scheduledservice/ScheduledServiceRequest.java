package com.tallermecanico.api.scheduledservice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduledServiceRequest(
        @NotNull(message = "Debes seleccionar un vehículo.") UUID vehicleId,
        @NotNull(message = "La fecha programada es obligatoria.") LocalDate scheduledDate,
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres.") String description
) {
}

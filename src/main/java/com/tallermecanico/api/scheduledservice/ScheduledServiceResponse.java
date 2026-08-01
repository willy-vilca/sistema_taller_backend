package com.tallermecanico.api.scheduledservice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledServiceResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientDni,
        UUID vehicleId,
        String licensePlate,
        String vehicleModel,
        String description,
        LocalDate scheduledDate,
        ScheduledServiceStatus status,
        UUID sourceServiceRecordId,
        UUID completedServiceRecordId,
        LocalDate completedServiceDate,
        Instant createdAt
) {
}

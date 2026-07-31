package com.tallermecanico.api.service;

import com.tallermecanico.api.user.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceRecordResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientDni,
        UUID vehicleId,
        String licensePlate,
        String vehicleModel,
        String description,
        LocalDate serviceDate,
        LocalDate nextServiceDate,
        BigDecimal totalCost,
        UserSummaryResponse responsibleUser,
        String notes
) {
}

package com.tallermecanico.api.client;

import java.util.UUID;

public record ClientSummaryResponse(
        UUID id,
        String fullName,
        String dni,
        String phone,
        String email,
        int vehicleCount,
        long serviceCount
) {
}

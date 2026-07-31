package com.tallermecanico.api.client;

import com.tallermecanico.api.vehicle.VehicleResponse;

import java.util.List;
import java.util.UUID;

public record ClientDetailResponse(
        UUID id,
        String fullName,
        String dni,
        String phone,
        String email,
        List<VehicleResponse> vehicles
) {
}

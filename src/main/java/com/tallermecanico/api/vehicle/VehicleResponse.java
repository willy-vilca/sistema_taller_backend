package com.tallermecanico.api.vehicle;

import java.util.UUID;

public record VehicleResponse(UUID id, UUID clientId, String licensePlate, String model) {
}

package com.tallermecanico.api.analytics;

import java.util.List;
import java.util.UUID;

public record AnalyticsFilterOptionsResponse(
        List<ClientOption> clients,
        List<VehicleOption> vehicles,
        List<EmployeeOption> employees
) {
    public record ClientOption(UUID id, String fullName, String dni) {
    }

    public record VehicleOption(UUID id, UUID clientId, String clientName, String licensePlate, String model) {
    }

    public record EmployeeOption(UUID id, String fullName, String username, boolean active) {
    }
}

package com.tallermecanico.api.vehicle;

public final class VehicleMapper {
    private VehicleMapper() {
    }

    public static VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getClient().getId(), vehicle.getLicensePlate(), vehicle.getModel());
    }
}

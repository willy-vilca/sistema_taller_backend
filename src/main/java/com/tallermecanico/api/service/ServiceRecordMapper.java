package com.tallermecanico.api.service;

import com.tallermecanico.api.client.Client;
import com.tallermecanico.api.user.UserMapper;
import com.tallermecanico.api.vehicle.Vehicle;

public final class ServiceRecordMapper {
    private ServiceRecordMapper() {
    }

    public static ServiceRecordResponse toResponse(ServiceRecord record) {
        Vehicle vehicle = record.getVehicle();
        Client client = vehicle.getClient();
        return new ServiceRecordResponse(
                record.getId(),
                client.getId(),
                client.getFullName(),
                client.getDni(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getModel(),
                record.getDescription(),
                record.getServiceDate(),
                record.getNextServiceDate(),
                record.getTotalCost(),
                UserMapper.toSummary(record.getResponsibleUser()),
                record.getNotes()
        );
    }
}

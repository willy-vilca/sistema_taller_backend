package com.tallermecanico.api.scheduledservice;

import com.tallermecanico.api.client.Client;
import com.tallermecanico.api.service.ServiceRecord;
import com.tallermecanico.api.vehicle.Vehicle;

public final class ScheduledServiceMapper {
    private ScheduledServiceMapper() {
    }

    public static ScheduledServiceResponse toResponse(ScheduledService scheduledService) {
        Vehicle vehicle = scheduledService.getVehicle();
        Client client = vehicle.getClient();
        ServiceRecord completedRecord = scheduledService.getCompletedServiceRecord();
        ServiceRecord sourceRecord = scheduledService.getSourceServiceRecord();

        return new ScheduledServiceResponse(
                scheduledService.getId(),
                client.getId(),
                client.getFullName(),
                client.getDni(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getModel(),
                scheduledService.getDescription(),
                scheduledService.getScheduledDate(),
                scheduledService.isCompleted() ? ScheduledServiceStatus.COMPLETED : ScheduledServiceStatus.PENDING,
                sourceRecord == null ? null : sourceRecord.getId(),
                completedRecord == null ? null : completedRecord.getId(),
                completedRecord == null ? null : completedRecord.getServiceDate(),
                scheduledService.getCreatedAt()
        );
    }
}

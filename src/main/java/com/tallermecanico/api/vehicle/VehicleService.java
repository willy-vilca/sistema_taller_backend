package com.tallermecanico.api.vehicle;

import com.tallermecanico.api.client.Client;
import com.tallermecanico.api.client.ClientService;
import com.tallermecanico.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final ClientService clientService;

    public VehicleService(VehicleRepository vehicleRepository, ClientService clientService) {
        this.vehicleRepository = vehicleRepository;
        this.clientService = clientService;
    }

    public VehicleResponse create(UUID clientId, VehicleRequest request) {
        String licensePlate = normalizePlate(request.licensePlate());
        if (vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate)) {
            throw new BusinessException(HttpStatus.CONFLICT, "La placa ya está registrada en el sistema.");
        }
        Client client = clientService.getEntity(clientId);
        Vehicle vehicle = vehicleRepository.save(new Vehicle(client, licensePlate, normalizeModel(request.model())));
        return VehicleMapper.toResponse(vehicle);
    }

    public VehicleResponse update(UUID id, VehicleRequest request) {
        Vehicle vehicle = getEntity(id);
        String licensePlate = normalizePlate(request.licensePlate());
        if (!vehicle.getLicensePlate().equalsIgnoreCase(licensePlate)
                && vehicleRepository.existsByLicensePlateIgnoreCase(licensePlate)) {
            throw new BusinessException(HttpStatus.CONFLICT, "La placa ya está registrada en el sistema.");
        }
        vehicle.setLicensePlate(licensePlate);
        vehicle.setModel(normalizeModel(request.model()));
        return VehicleMapper.toResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public Vehicle getEntity(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el vehículo solicitado."));
    }

    private String normalizePlate(String plate) {
        return plate.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeModel(String model) {
        return model.trim().replaceAll("\\s+", " ");
    }
}

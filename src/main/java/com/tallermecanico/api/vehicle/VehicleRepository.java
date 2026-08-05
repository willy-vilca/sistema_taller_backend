package com.tallermecanico.api.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    @EntityGraph(attributePaths = "client")
    List<Vehicle> findAllByOrderByLicensePlateAsc();
}

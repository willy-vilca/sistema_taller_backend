package com.tallermecanico.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByDni(String dni);

    Optional<Client> findByDni(String dni);

    List<Client> findAllByOrderByFullNameAsc();

    @Query(
            value = """
                    select distinct client from Client client
                    left join client.vehicles vehicle
                    where lower(client.fullName) like concat('%', :searchPattern, '%')
                       or lower(client.dni) like concat('%', :searchPattern, '%')
                       or replace(replace(lower(vehicle.licensePlate), '-', ''), ' ', '') like concat('%', :plateSearchPattern, '%')
                    """,
            countQuery = """
                    select count(distinct client) from Client client
                    left join client.vehicles vehicle
                    where lower(client.fullName) like concat('%', :searchPattern, '%')
                       or lower(client.dni) like concat('%', :searchPattern, '%')
                       or replace(replace(lower(vehicle.licensePlate), '-', ''), ' ', '') like concat('%', :plateSearchPattern, '%')
                    """
    )
    Page<Client> search(
            @Param("searchPattern") String searchPattern,
            @Param("plateSearchPattern") String plateSearchPattern,
            Pageable pageable
    );
}

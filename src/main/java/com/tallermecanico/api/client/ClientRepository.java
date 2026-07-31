package com.tallermecanico.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByDni(String dni);

    Optional<Client> findByDni(String dni);

    @Query(
            value = """
                    select distinct client from Client client
                    left join client.vehicles vehicle
                    where lower(client.fullName) like lower(concat('%', :search, '%'))
                       or client.dni like concat('%', :search, '%')
                       or upper(vehicle.licensePlate) like upper(concat('%', :search, '%'))
                    """,
            countQuery = """
                    select count(distinct client) from Client client
                    left join client.vehicles vehicle
                    where lower(client.fullName) like lower(concat('%', :search, '%'))
                       or client.dni like concat('%', :search, '%')
                       or upper(vehicle.licensePlate) like upper(concat('%', :search, '%'))
                    """
    )
    Page<Client> search(@Param("search") String search, Pageable pageable);
}

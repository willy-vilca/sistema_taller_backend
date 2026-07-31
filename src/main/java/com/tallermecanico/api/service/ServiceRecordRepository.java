package com.tallermecanico.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, UUID> {

    @Query("""
            select serviceRecord from ServiceRecord serviceRecord
            join serviceRecord.vehicle vehicle
            join vehicle.client client
            join serviceRecord.responsibleUser responsibleUser
            where (lower(client.fullName) like lower(concat('%', :search, '%'))
                    or upper(vehicle.licensePlate) like upper(concat('%', :search, '%'))
                    or lower(serviceRecord.description) like lower(concat('%', :search, '%'))
                    or lower(responsibleUser.fullName) like lower(concat('%', :search, '%')))
              and (:vehicleId is null or vehicle.id = :vehicleId)
              and (:fromDate is null or serviceRecord.serviceDate >= :fromDate)
              and (:toDate is null or serviceRecord.serviceDate <= :toDate)
            """)
    Page<ServiceRecord> search(
            @Param("search") String search,
            @Param("vehicleId") UUID vehicleId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("select coalesce(sum(serviceRecord.totalCost), 0) from ServiceRecord serviceRecord")
    BigDecimal totalRevenue();
}

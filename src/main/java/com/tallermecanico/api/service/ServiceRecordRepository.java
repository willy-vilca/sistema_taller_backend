package com.tallermecanico.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, UUID>, JpaSpecificationExecutor<ServiceRecord> {

    @Override
    @EntityGraph(attributePaths = {"vehicle", "vehicle.client", "responsibleUser"})
    Page<ServiceRecord> findAll(org.springframework.data.jpa.domain.Specification<ServiceRecord> specification, Pageable pageable);

    @Query("select coalesce(sum(serviceRecord.totalCost), 0) from ServiceRecord serviceRecord")
    BigDecimal totalRevenue();
}

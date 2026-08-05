package com.tallermecanico.api.scheduledservice;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

public interface ScheduledServiceRepository extends JpaRepository<ScheduledService, UUID>, JpaSpecificationExecutor<ScheduledService> {

    @Override
    @EntityGraph(attributePaths = {"vehicle", "vehicle.client", "sourceServiceRecord", "completedServiceRecord"})
    Page<ScheduledService> findAll(org.springframework.data.jpa.domain.Specification<ScheduledService> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.client", "sourceServiceRecord", "completedServiceRecord"})
    @Query("select scheduledService from ScheduledService scheduledService where scheduledService.id = :id")
    Optional<ScheduledService> findOneDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select scheduledService from ScheduledService scheduledService where scheduledService.id = :id")
    Optional<ScheduledService> findByIdForCompletion(@Param("id") UUID id);

    long countByCompletedServiceRecordIsNull();

    long countByCompletedServiceRecordIsNullAndScheduledDateBefore(LocalDate date);

    @EntityGraph(attributePaths = {"vehicle", "vehicle.client", "sourceServiceRecord", "completedServiceRecord"})
    List<ScheduledService> findTop5ByCompletedServiceRecordIsNullOrderByScheduledDateAsc();

    @Query("select scheduledService.completedServiceRecord.id, scheduledService.id from ScheduledService scheduledService where scheduledService.completedServiceRecord.id in :serviceRecordIds")
    List<Object[]> findCompletedServiceLinks(@Param("serviceRecordIds") List<UUID> serviceRecordIds);
}

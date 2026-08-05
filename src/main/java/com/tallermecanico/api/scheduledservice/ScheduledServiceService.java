package com.tallermecanico.api.scheduledservice;

import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.common.PageResponse;
import com.tallermecanico.api.service.ServiceRecord;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import com.tallermecanico.api.vehicle.Vehicle;
import com.tallermecanico.api.vehicle.VehicleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduledServiceService {
    private final ScheduledServiceRepository scheduledServiceRepository;
    private final VehicleRepository vehicleRepository;
    private final SystemUserRepository userRepository;

    public ScheduledServiceService(
            ScheduledServiceRepository scheduledServiceRepository,
            VehicleRepository vehicleRepository,
            SystemUserRepository userRepository
    ) {
        this.scheduledServiceRepository = scheduledServiceRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduledServiceResponse> search(
            String search,
            UUID vehicleId,
            LocalDate fromDate,
            LocalDate toDate,
            ScheduledServiceStatus status,
            int page,
            int size
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(
                scheduledServiceRepository.findAll(buildSearchSpecification(search, vehicleId, fromDate, toDate, status), pageable),
                ScheduledServiceMapper::toResponse
        );
    }

    @Transactional(readOnly = true)
    public ScheduledServiceResponse get(UUID id) {
        return ScheduledServiceMapper.toResponse(getDetailedEntity(id));
    }

    public ScheduledServiceResponse create(ScheduledServiceRequest request, String createdByUsername) {
        Vehicle vehicle = getVehicle(request.vehicleId());
        SystemUser createdByUser = getActiveUserByUsername(createdByUsername);
        ScheduledService scheduledService = new ScheduledService(
                vehicle,
                null,
                createdByUser,
                nullableText(request.description()),
                request.scheduledDate()
        );
        return ScheduledServiceMapper.toResponse(scheduledServiceRepository.save(scheduledService));
    }

    public ScheduledServiceResponse update(UUID id, ScheduledServiceRequest request) {
        ScheduledService scheduledService = getDetailedEntity(id);
        ensurePending(scheduledService);

        Vehicle vehicle = getVehicle(request.vehicleId());
        ServiceRecord sourceServiceRecord = scheduledService.getSourceServiceRecord();
        if (sourceServiceRecord != null) {
            if (sourceServiceRecord.getVehicle().getId().equals(vehicle.getId())) {
                sourceServiceRecord.setNextServiceDate(request.scheduledDate());
            } else {
                // La programación deja de corresponder al servicio de origen al cambiar de vehículo.
                sourceServiceRecord.setNextServiceDate(null);
                scheduledService.setSourceServiceRecord(null);
            }
        }

        scheduledService.setVehicle(vehicle);
        scheduledService.setScheduledDate(request.scheduledDate());
        scheduledService.setDescription(nullableText(request.description()));
        return ScheduledServiceMapper.toResponse(scheduledService);
    }

    public void deletePending(UUID id) {
        ScheduledService scheduledService = getDetailedEntity(id);
        ensurePending(scheduledService);

        if (scheduledService.getSourceServiceRecord() != null) {
            scheduledService.getSourceServiceRecord().setNextServiceDate(null);
        }
        scheduledServiceRepository.delete(scheduledService);
    }

    public void createFromServiceRecord(
            ServiceRecord sourceServiceRecord,
            SystemUser createdByUser,
            LocalDate scheduledDate,
            String description
    ) {
        ScheduledService scheduledService = new ScheduledService(
                sourceServiceRecord.getVehicle(),
                sourceServiceRecord,
                createdByUser,
                normalizeText(description),
                scheduledDate
        );
        scheduledServiceRepository.save(scheduledService);
    }

    public ScheduledService reserveForCompletion(UUID id, UUID expectedVehicleId) {
        ScheduledService scheduledService = scheduledServiceRepository.findByIdForCompletion(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el servicio programado seleccionado."));

        if (scheduledService.isCompleted()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Este servicio programado ya fue completado por otro registro.");
        }
        if (!scheduledService.getVehicle().getId().equals(expectedVehicleId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El servicio programado no corresponde al vehículo seleccionado.");
        }
        return scheduledService;
    }

    public void complete(ScheduledService scheduledService, ServiceRecord completedServiceRecord) {
        scheduledService.setCompletedServiceRecord(completedServiceRecord);
    }

    private ScheduledService getDetailedEntity(UUID id) {
        return scheduledServiceRepository.findOneDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el servicio programado solicitado."));
    }

    private Vehicle getVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el vehículo seleccionado."));
    }

    private void ensurePending(ScheduledService scheduledService) {
        if (scheduledService.isCompleted()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Los servicios programados completados no se pueden editar ni eliminar."
            );
        }
    }

    private SystemUser getActiveUserByUsername(String username) {
        SystemUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "El usuario que registra el servicio está inactivo.");
        }
        return user;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private Specification<ScheduledService> buildSearchSpecification(
            String search,
            UUID vehicleId,
            LocalDate fromDate,
            LocalDate toDate,
            ScheduledServiceStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            var vehicle = root.join("vehicle");
            var client = vehicle.join("client");

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(client.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(vehicle.get("licensePlate")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(vehicle.get("model")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
                ));
            }
            if (vehicleId != null) {
                predicates.add(criteriaBuilder.equal(vehicle.get("id"), vehicleId));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("scheduledDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("scheduledDate"), toDate));
            }
            if (status == ScheduledServiceStatus.PENDING) {
                predicates.add(criteriaBuilder.isNull(root.get("completedServiceRecord")));
            }
            if (status == ScheduledServiceStatus.COMPLETED) {
                predicates.add(criteriaBuilder.isNotNull(root.get("completedServiceRecord")));
            }

            if (!Long.class.equals(query.getResultType())) {
                var pendingFirst = criteriaBuilder.selectCase()
                        .when(criteriaBuilder.isNull(root.get("completedServiceRecord")), 0)
                        .otherwise(1);
                query.orderBy(
                        criteriaBuilder.asc(pendingFirst),
                        criteriaBuilder.asc(root.get("scheduledDate")),
                        criteriaBuilder.desc(root.get("createdAt"))
                );
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String nullableText(String value) {
        return value == null || value.isBlank() ? null : normalizeText(value);
    }
}

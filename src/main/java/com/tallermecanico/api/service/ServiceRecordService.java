package com.tallermecanico.api.service;

import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.common.PageResponse;
import com.tallermecanico.api.client.ClientRepository;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import com.tallermecanico.api.user.UserService;
import com.tallermecanico.api.vehicle.Vehicle;
import com.tallermecanico.api.vehicle.VehicleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ServiceRecordService {
    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final SystemUserRepository userRepository;
    private final ClientRepository clientRepository;

    public ServiceRecordService(
            ServiceRecordRepository serviceRecordRepository,
            VehicleRepository vehicleRepository,
            SystemUserRepository userRepository,
            ClientRepository clientRepository
    ) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceRecordResponse> search(
            String search,
            UUID vehicleId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = PageRequest.of(page, size, Sort.by("serviceDate").descending().and(Sort.by("createdAt").descending()));
        return PageResponse.from(
                serviceRecordRepository.search(normalizeSearch(search), vehicleId, fromDate, toDate, pageable),
                ServiceRecordMapper::toResponse
        );
    }

    public ServiceRecordResponse create(ServiceRecordRequest request, String registeredByUsername) {
        validateNextServiceDate(request.serviceDate(), request.nextServiceDate());
        Vehicle vehicle = getVehicle(request.vehicleId());
        SystemUser responsibleUser = getActiveUser(request.responsibleUserId());
        SystemUser registeredByUser = userRepository.findByUsernameIgnoreCase(registeredByUsername)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida."));

        ServiceRecord record = new ServiceRecord(
                vehicle,
                responsibleUser,
                registeredByUser,
                normalizeText(request.description()),
                request.serviceDate(),
                request.nextServiceDate(),
                request.totalCost(),
                nullableText(request.notes())
        );
        return ServiceRecordMapper.toResponse(serviceRecordRepository.save(record));
    }

    public ServiceRecordResponse update(UUID id, ServiceRecordRequest request) {
        validateNextServiceDate(request.serviceDate(), request.nextServiceDate());
        ServiceRecord record = getEntity(id);
        record.setVehicle(getVehicle(request.vehicleId()));
        record.setResponsibleUser(getActiveUser(request.responsibleUserId()));
        record.setDescription(normalizeText(request.description()));
        record.setServiceDate(request.serviceDate());
        record.setNextServiceDate(request.nextServiceDate());
        record.setTotalCost(request.totalCost());
        record.setNotes(nullableText(request.notes()));
        return ServiceRecordMapper.toResponse(record);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal revenue = serviceRecordRepository.totalRevenue();
        return new DashboardStatsResponse(
                clientRepository.count(),
                vehicleRepository.count(),
                serviceRecordRepository.count(),
                revenue == null ? BigDecimal.ZERO : revenue
        );
    }

    @Transactional(readOnly = true)
    public ServiceRecord getEntity(UUID id) {
        return serviceRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el servicio solicitado."));
    }

    private Vehicle getVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el vehículo seleccionado."));
    }

    private SystemUser getActiveUser(UUID id) {
        SystemUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el mecánico seleccionado."));
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "El mecánico seleccionado está inactivo.");
        }
        return user;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private void validateNextServiceDate(LocalDate serviceDate, LocalDate nextServiceDate) {
        if (nextServiceDate != null && nextServiceDate.isBefore(serviceDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha del próximo servicio no puede ser anterior al servicio realizado.");
        }
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? "" : search.trim();
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

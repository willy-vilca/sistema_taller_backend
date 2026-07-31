package com.tallermecanico.api.service;

import com.tallermecanico.api.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping
    public PageResponse<ServiceRecordResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return serviceRecordService.search(search, vehicleId, fromDate, toDate, page, size);
    }

    @GetMapping("/stats")
    public DashboardStatsResponse stats() {
        return serviceRecordService.getDashboardStats();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRecordResponse create(@Valid @RequestBody ServiceRecordRequest request, Authentication authentication) {
        return serviceRecordService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public ServiceRecordResponse update(@PathVariable UUID id, @Valid @RequestBody ServiceRecordRequest request) {
        return serviceRecordService.update(id, request);
    }
}

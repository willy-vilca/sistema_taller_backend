package com.tallermecanico.api.scheduledservice;

import com.tallermecanico.api.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/scheduled-services")
public class ScheduledServiceController {
    private final ScheduledServiceService scheduledServiceService;

    public ScheduledServiceController(ScheduledServiceService scheduledServiceService) {
        this.scheduledServiceService = scheduledServiceService;
    }

    @GetMapping
    public PageResponse<ScheduledServiceResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) ScheduledServiceStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size
    ) {
        return scheduledServiceService.search(search, vehicleId, fromDate, toDate, status, page, size);
    }

    @GetMapping("/{id}")
    public ScheduledServiceResponse get(@PathVariable UUID id) {
        return scheduledServiceService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledServiceResponse create(
            @Valid @RequestBody ScheduledServiceRequest request,
            Authentication authentication
    ) {
        return scheduledServiceService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public ScheduledServiceResponse update(@PathVariable UUID id, @Valid @RequestBody ScheduledServiceRequest request) {
        return scheduledServiceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        scheduledServiceService.deletePending(id);
    }
}

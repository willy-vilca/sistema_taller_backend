package com.tallermecanico.api.analytics;

import com.tallermecanico.api.common.PageResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public AnalyticsDashboardResponse dashboard(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return analyticsService.getDashboard(fromDate, toDate);
    }

    @GetMapping("/services")
    public PageResponse<AnalyticsServiceRecordResponse> searchServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID responsibleUserId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minCost,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxCost,
            @RequestParam(required = false) AnalyticsServiceOrigin origin,
            @RequestParam(required = false) Boolean hasNextService,
            @RequestParam(required = false) AnalyticsServiceSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return analyticsService.searchServices(
                new AnalyticsServiceFilters(search, clientId, vehicleId, responsibleUserId, fromDate, toDate, minCost, maxCost, origin, hasNextService, sort),
                page,
                size
        );
    }

    @GetMapping("/services/summary")
    public AnalyticsSummaryResponse summarizeServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID responsibleUserId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minCost,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxCost,
            @RequestParam(required = false) AnalyticsServiceOrigin origin,
            @RequestParam(required = false) Boolean hasNextService
    ) {
        return analyticsService.summarizeServices(
                new AnalyticsServiceFilters(search, clientId, vehicleId, responsibleUserId, fromDate, toDate, minCost, maxCost, origin, hasNextService, null)
        );
    }

    @GetMapping("/filter-options")
    public AnalyticsFilterOptionsResponse filterOptions() {
        return analyticsService.getFilterOptions();
    }
}

package com.tallermecanico.api.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnalyticsServiceFilters(
        String search,
        UUID clientId,
        UUID vehicleId,
        UUID responsibleUserId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal minCost,
        BigDecimal maxCost,
        AnalyticsServiceOrigin origin,
        Boolean hasNextService,
        AnalyticsServiceSort sort
) {
}

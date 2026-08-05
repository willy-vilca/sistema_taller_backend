package com.tallermecanico.api.analytics;

import com.tallermecanico.api.scheduledservice.ScheduledServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsDashboardResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long serviceCount,
        BigDecimal revenue,
        BigDecimal averageTicket,
        long newServiceCount,
        long scheduledCompletionCount,
        long pendingScheduledServiceCount,
        long overdueScheduledServiceCount,
        List<AnalyticsTrendResponse> trend,
        List<AnalyticsRankingResponse> topClients,
        List<AnalyticsRankingResponse> topVehicles,
        List<AnalyticsRankingResponse> topEmployees,
        List<ScheduledServiceResponse> priorityScheduledServices
) {
}

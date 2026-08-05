package com.tallermecanico.api.analytics;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        long serviceCount,
        BigDecimal revenue,
        BigDecimal averageTicket,
        long newServiceCount,
        long scheduledCompletionCount
) {
}

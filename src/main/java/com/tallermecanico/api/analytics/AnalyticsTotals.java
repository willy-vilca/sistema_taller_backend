package com.tallermecanico.api.analytics;

import java.math.BigDecimal;

public record AnalyticsTotals(
        long serviceCount,
        BigDecimal revenue,
        BigDecimal averageTicket,
        long scheduledCompletionCount
) {
}

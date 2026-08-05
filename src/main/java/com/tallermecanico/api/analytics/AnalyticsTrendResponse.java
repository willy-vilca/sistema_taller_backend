package com.tallermecanico.api.analytics;

import java.math.BigDecimal;

public record AnalyticsTrendResponse(
        String period,
        long serviceCount,
        BigDecimal revenue
) {
}

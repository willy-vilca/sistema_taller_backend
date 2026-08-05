package com.tallermecanico.api.analytics;

import java.math.BigDecimal;
import java.util.UUID;

public record AnalyticsRankingResponse(
        UUID id,
        String label,
        String secondaryLabel,
        long serviceCount,
        BigDecimal revenue
) {
}

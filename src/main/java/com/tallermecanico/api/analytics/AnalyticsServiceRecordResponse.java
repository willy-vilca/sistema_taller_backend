package com.tallermecanico.api.analytics;

import com.tallermecanico.api.service.ServiceRecordResponse;

import java.util.UUID;

public record AnalyticsServiceRecordResponse(
        ServiceRecordResponse service,
        AnalyticsServiceOrigin origin,
        UUID completedScheduledServiceId
) {
}

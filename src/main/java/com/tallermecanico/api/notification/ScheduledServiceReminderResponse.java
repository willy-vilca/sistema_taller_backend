package com.tallermecanico.api.notification;

import java.time.LocalDate;

public record ScheduledServiceReminderResponse(
        LocalDate scheduledDate,
        int scheduledServiceCount,
        int eligibleAdminCount,
        int sentCount,
        int alreadySentCount,
        int failedCount
) {
}

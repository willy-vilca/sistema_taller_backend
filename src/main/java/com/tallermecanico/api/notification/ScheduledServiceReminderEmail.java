package com.tallermecanico.api.notification;

import com.tallermecanico.api.scheduledservice.ScheduledService;
import com.tallermecanico.api.user.SystemUser;

import java.time.LocalDate;
import java.util.List;

public record ScheduledServiceReminderEmail(SystemUser recipient, LocalDate scheduledDate, List<ScheduledService> scheduledServices) {
}

package com.tallermecanico.api.notification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/notifications")
public class ScheduledServiceReminderController {
    private final ScheduledServiceReminderService reminderService;

    public ScheduledServiceReminderController(ScheduledServiceReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/tomorrow-scheduled-services")
    public ScheduledServiceReminderResponse dispatchTomorrowReminder(
            @RequestHeader(value = "X-Notification-Secret", required = false) String requestSecret
    ) {
        return reminderService.dispatchTomorrowReminder(requestSecret);
    }
}

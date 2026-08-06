package com.tallermecanico.api.notification;

import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.scheduledservice.ScheduledService;
import com.tallermecanico.api.scheduledservice.ScheduledServiceRepository;
import com.tallermecanico.api.user.RoleName;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ScheduledServiceReminderService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledServiceReminderService.class);
    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("America/Lima");

    private final NotificationProperties properties;
    private final ScheduledServiceRepository scheduledServiceRepository;
    private final SystemUserRepository userRepository;
    private final NotificationDeliveryService deliveryService;
    private final AzureCommunicationEmailSender emailSender;

    public ScheduledServiceReminderService(
            NotificationProperties properties,
            ScheduledServiceRepository scheduledServiceRepository,
            SystemUserRepository userRepository,
            NotificationDeliveryService deliveryService,
            AzureCommunicationEmailSender emailSender
    ) {
        this.properties = properties;
        this.scheduledServiceRepository = scheduledServiceRepository;
        this.userRepository = userRepository;
        this.deliveryService = deliveryService;
        this.emailSender = emailSender;
    }

    public ScheduledServiceReminderResponse dispatchTomorrowReminder(String requestSecret) {
        verifyRequest(requestSecret);
        LocalDate tomorrow = LocalDate.now(BUSINESS_TIME_ZONE).plusDays(1);
        List<ScheduledService> scheduledServices = scheduledServiceRepository.findPendingDetailedByScheduledDate(tomorrow);
        List<SystemUser> recipients = userRepository.findActiveAdminsWithScheduledServiceNotifications();

        if (scheduledServices.isEmpty()) {
            return new ScheduledServiceReminderResponse(tomorrow, 0, recipients.size(), 0, 0, 0);
        }

        int sentCount = 0;
        int alreadySentCount = 0;
        int failedCount = 0;
        for (SystemUser recipient : recipients) {
            NotificationDeliveryService.DeliveryReservation reservation = deliveryService.reserve(
                    tomorrow,
                    recipient,
                    scheduledServices.size()
            );
            if (reservation.status() == NotificationDeliveryService.ReservationStatus.ALREADY_SENT
                    || reservation.status() == NotificationDeliveryService.ReservationStatus.IN_PROGRESS) {
                alreadySentCount++;
                continue;
            }
            try {
                String providerOperationId = emailSender.send(new ScheduledServiceReminderEmail(recipient, tomorrow, scheduledServices));
                deliveryService.markSent(reservation.deliveryId(), providerOperationId);
                sentCount++;
            } catch (RuntimeException exception) {
                log.error("No se pudo enviar el recordatorio de servicios programados al usuario {}.", recipient.getId(), exception);
                deliveryService.markFailed(reservation.deliveryId(), exception.getMessage());
                failedCount++;
            }
        }
        return new ScheduledServiceReminderResponse(
                tomorrow,
                scheduledServices.size(),
                recipients.size(),
                sentCount,
                alreadySentCount,
                failedCount
        );
    }

    private void verifyRequest(String requestSecret) {
        if (!properties.enabled()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Las notificaciones por correo no están activadas.");
        }
        if (!properties.hasCompleteEmailConfiguration()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "La configuración de correo para notificaciones está incompleta.");
        }
        String configuredSecret = properties.internalSecret();
        if (!hasText(configuredSecret) || !hasText(requestSecret)
                || !MessageDigest.isEqual(
                configuredSecret.getBytes(StandardCharsets.UTF_8),
                requestSecret.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No se autorizó la ejecución de recordatorios.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

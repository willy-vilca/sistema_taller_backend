package com.tallermecanico.api.notification;

import com.tallermecanico.api.user.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class NotificationDeliveryService {
    private final ScheduledServiceNotificationDeliveryRepository deliveryRepository;

    public NotificationDeliveryService(ScheduledServiceNotificationDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryReservation reserve(LocalDate notificationDate, SystemUser recipient, int scheduledServiceCount) {
        ScheduledServiceNotificationDelivery delivery = deliveryRepository
                .findByNotificationDateAndRecipientUserId(notificationDate, recipient.getId())
                .orElse(null);

        if (delivery != null && delivery.getStatus() == NotificationDeliveryStatus.SENT) {
            return DeliveryReservation.alreadySent();
        }
        if (delivery != null && delivery.getStatus() == NotificationDeliveryStatus.PROCESSING) {
            return DeliveryReservation.inProgress();
        }
        if (delivery == null) {
            delivery = new ScheduledServiceNotificationDelivery(recipient, notificationDate, scheduledServiceCount);
            deliveryRepository.save(delivery);
        } else {
            delivery.prepareRetry(scheduledServiceCount);
        }
        return DeliveryReservation.reserved(delivery.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID deliveryId, String providerOperationId) {
        ScheduledServiceNotificationDelivery delivery = getById(deliveryId);
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setProviderOperationId(providerOperationId);
        delivery.setErrorMessage(null);
        delivery.setSentAt(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID deliveryId, String errorMessage) {
        ScheduledServiceNotificationDelivery delivery = getById(deliveryId);
        delivery.setStatus(NotificationDeliveryStatus.FAILED);
        delivery.setErrorMessage(truncate(errorMessage));
    }

    private ScheduledServiceNotificationDelivery getById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("No se encontró el registro de envío de notificación."));
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "No se pudo enviar el correo de recordatorio.";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public record DeliveryReservation(UUID deliveryId, ReservationStatus status) {
        static DeliveryReservation reserved(UUID deliveryId) {
            return new DeliveryReservation(deliveryId, ReservationStatus.RESERVED);
        }

        static DeliveryReservation alreadySent() {
            return new DeliveryReservation(null, ReservationStatus.ALREADY_SENT);
        }

        static DeliveryReservation inProgress() {
            return new DeliveryReservation(null, ReservationStatus.IN_PROGRESS);
        }
    }

    public enum ReservationStatus {
        RESERVED,
        ALREADY_SENT,
        IN_PROGRESS
    }
}

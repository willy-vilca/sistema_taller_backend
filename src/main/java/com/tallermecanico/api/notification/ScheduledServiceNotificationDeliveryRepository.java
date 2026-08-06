package com.tallermecanico.api.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledServiceNotificationDeliveryRepository extends JpaRepository<ScheduledServiceNotificationDelivery, UUID> {
    Optional<ScheduledServiceNotificationDelivery> findByNotificationDateAndRecipientUserId(LocalDate notificationDate, UUID recipientUserId);
}

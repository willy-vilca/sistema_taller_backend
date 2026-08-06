package com.tallermecanico.api.notification;

import com.tallermecanico.api.user.SystemUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "scheduled_service_notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scheduled_service_notification_recipient_date",
                columnNames = {"recipient_user_id", "notification_date"}
        ),
        indexes = @Index(name = "idx_scheduled_service_notification_date", columnList = "notification_date")
)
@Getter
@Setter
@NoArgsConstructor
public class ScheduledServiceNotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private SystemUser recipientUser;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "scheduled_service_count", nullable = false)
    private int scheduledServiceCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "provider_operation_id", length = 150)
    private String providerOperationId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ScheduledServiceNotificationDelivery(SystemUser recipientUser, LocalDate notificationDate, int scheduledServiceCount) {
        this.recipientUser = recipientUser;
        this.notificationDate = notificationDate;
        this.scheduledServiceCount = scheduledServiceCount;
        this.status = NotificationDeliveryStatus.PROCESSING;
        this.attemptCount = 1;
    }

    public void prepareRetry(int scheduledServiceCount) {
        this.scheduledServiceCount = scheduledServiceCount;
        this.status = NotificationDeliveryStatus.PROCESSING;
        this.errorMessage = null;
        this.providerOperationId = null;
        this.sentAt = null;
        this.attemptCount++;
    }
}

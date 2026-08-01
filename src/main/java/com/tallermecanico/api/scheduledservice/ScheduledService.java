package com.tallermecanico.api.scheduledservice;

import com.tallermecanico.api.service.ServiceRecord;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
        name = "scheduled_services",
        indexes = {
                @Index(name = "idx_scheduled_services_vehicle_date", columnList = "vehicle_id,scheduled_date"),
                @Index(name = "idx_scheduled_services_date", columnList = "scheduled_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ScheduledService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_service_record_id", unique = true)
    private ServiceRecord sourceServiceRecord;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_service_record_id", unique = true)
    private ServiceRecord completedServiceRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private SystemUser createdByUser;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public ScheduledService(
            Vehicle vehicle,
            ServiceRecord sourceServiceRecord,
            SystemUser createdByUser,
            String description,
            LocalDate scheduledDate
    ) {
        this.vehicle = vehicle;
        this.sourceServiceRecord = sourceServiceRecord;
        this.createdByUser = createdByUser;
        this.description = description;
        this.scheduledDate = scheduledDate;
    }

    public boolean isCompleted() {
        return completedServiceRecord != null;
    }
}

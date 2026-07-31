package com.tallermecanico.api.service;

import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "service_records")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private SystemUser responsibleUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_user_id", nullable = false)
    private SystemUser registeredByUser;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate serviceDate;

    private LocalDate nextServiceDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 2000)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public ServiceRecord(
            Vehicle vehicle,
            SystemUser responsibleUser,
            SystemUser registeredByUser,
            String description,
            LocalDate serviceDate,
            LocalDate nextServiceDate,
            BigDecimal totalCost,
            String notes
    ) {
        this.vehicle = vehicle;
        this.responsibleUser = responsibleUser;
        this.registeredByUser = registeredByUser;
        this.description = description;
        this.serviceDate = serviceDate;
        this.nextServiceDate = nextServiceDate;
        this.totalCost = totalCost;
        this.notes = notes;
    }
}
